# Requiere MySQL 8.4 aislado en 33307 y pipeline-http-local.cjs en 19090/19091.
$ErrorActionPreference='Stop'
$taskRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location -LiteralPath $taskRoot
$taskBin=Join-Path $taskRoot '.tools\mysql84\bin\mysql.exe'
function Sql([string]$Query) {
    $result=& $taskBin --no-defaults --host=127.0.0.1 --port=33307 --user=root --batch --raw --skip-column-names --database=ep360_incremento6_restart --execute=$Query
    if($LASTEXITCODE -ne 0){throw 'SQL de prueba falló.'}
    return ($result -join "`n").Trim()
}
function Control($Data) { $null=Invoke-RestMethod 'http://127.0.0.1:19091/__control' -Method Put -ContentType application/json -Body ($Data|ConvertTo-Json) }
function Iniciar([string]$Nombre) {
    $taskJar=Join-Path $taskRoot 'backend\target\entrevista-pro-360-0.1.0-SNAPSHOT.jar'
    $p=Start-Process -FilePath 'C:\Program Files\Java\jdk-17\bin\java.exe' -ArgumentList @('-jar',"`"$taskJar`"",'--server.port=18081','--integraciones.worker-intervalo-ms=500') -WindowStyle Hidden -RedirectStandardOutput ".tools\inc6-$Nombre-runtime.log" -RedirectStandardError ".tools\inc6-$Nombre-err.log" -PassThru
    try {
        for($i=0;$i -lt 60;$i++){
            if($p.HasExited){throw 'JVM terminó antes de estar lista.'}
            try{$null=Invoke-RestMethod 'http://127.0.0.1:18081/api/v1/puestos' -TimeoutSec 2;return $p}catch{Start-Sleep -Seconds 1}
        }
        throw 'JVM no respondió.'
    }catch{if(-not $p.HasExited){Stop-Process -Id $p.Id};throw}
}
& $taskBin --no-defaults --host=127.0.0.1 --port=33307 --user=root --execute='CREATE DATABASE IF NOT EXISTS ep360_incremento6_restart CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
if($LASTEXITCODE -ne 0){throw 'No se creó base aislada.'}
$env:DB_URL='jdbc:mysql://127.0.0.1:33307/ep360_incremento6_restart?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:DB_USERNAME='root';$env:DB_PASSWORD=''
$env:S3_BUCKET='ep360-restart-local';$env:S3_ENDPOINT_OVERRIDE='http://127.0.0.1:19090';$env:AWS_REGION='us-east-1'
$env:AWS_ACCESS_KEY_ID='ep360-local-test';$env:AWS_SECRET_ACCESS_KEY='ep360-local-test-secret'
$env:OPENAI_API_KEY='clave-local';$env:OPENAI_BASE_URL='http://127.0.0.1:19091'
$env:FFMPEG_BIN=Join-Path $taskRoot '.tools\ffmpeg\ffmpeg.exe';$env:FFPROBE_BIN=Join-Path $taskRoot '.tools\ffmpeg\ffprobe.exe'
Remove-Item Env:JWT_SECRET -ErrorAction SilentlyContinue
$taskPrimero=$null;$taskSegundo=$null
try {
    Control @{demora=15000}
    $taskPrevio=@((Invoke-RestMethod 'http://127.0.0.1:19091/__audit').peticiones).Count
    $taskPrimero=Iniciar 'jvm-antes'
    $taskEmail="restart-$([guid]::NewGuid())@example.test"
    $taskPass='Durable-local-2026!'
    $registro=Invoke-RestMethod 'http://127.0.0.1:18081/api/v1/auth/registro' -Method Post -ContentType application/json -Body (@{nombre_completo='Prueba Reinicio';email=$taskEmail;password=$taskPass}|ConvertTo-Json)
    $login=Invoke-RestMethod 'http://127.0.0.1:18081/api/v1/auth/login' -Method Post -ContentType application/json -Body (@{email=$taskEmail;password=$taskPass}|ConvertTo-Json)
    $headers=@{Authorization="Bearer $($login.access_token)"}
    $puestos=Invoke-RestMethod 'http://127.0.0.1:18081/api/v1/puestos'
    $sesion=Invoke-RestMethod 'http://127.0.0.1:18081/api/v1/sesiones' -Method Post -Headers $headers -ContentType application/json -Body (@{id_puesto=$puestos[0].id_puesto}|ConvertTo-Json)
    $preguntas=Invoke-RestMethod "http://127.0.0.1:18081/api/v1/puestos/$($puestos[0].id_puesto)/preguntas"
    $video=Join-Path $taskRoot '.tools\inc6-restart-video.webm'
    & $env:FFMPEG_BIN -y -v error -f lavfi -i 'testsrc=size=64x64:rate=10' -f lavfi -i 'sine=frequency=440:sample_rate=48000' -t 2 -threads 1 -filter_threads 1 -c:v libvpx -pix_fmt yuv420p -c:a libopus $video
    if($LASTEXITCODE -ne 0){throw 'Fixture multimedia falló.'}
    $subida=Invoke-RestMethod "http://127.0.0.1:18081/api/v1/sesiones/$($sesion.id_sesion)/preguntas/$($preguntas[0].id_pregunta)/respuesta" -Method Put -Headers $headers -Form @{video=Get-Item -LiteralPath $video}
    $taskId=$subida.id_respuesta
    $listo=$false
    for($i=0;$i -lt 80;$i++){
        $enCurso=Sql "SELECT COUNT(*) FROM respuestas_video WHERE id_respuesta=$taskId AND JSON_UNQUOTE(JSON_EXTRACT(procesamiento_contexto,'$.etapa_actual'))='TRANSCRIBIR' AND JSON_EXTRACT(procesamiento_contexto,'$.bloqueo_hasta_utc') <> CAST('null' AS JSON)"
        $llamadas=@((Invoke-RestMethod 'http://127.0.0.1:19091/__audit').peticiones).Count
        if($enCurso -eq '1' -and $llamadas -gt $taskPrevio){$listo=$true;break}
        Start-Sleep -Milliseconds 100
    }
    if(-not $listo){throw 'No se alcanzó STT en curso.'}
    $antes=Sql "SELECT procesamiento_contexto FROM respuestas_video WHERE id_respuesta=$taskId" | ConvertFrom-Json
    Stop-Process -Id $taskPrimero.Id
    $taskPrimero.WaitForExit()
    if($antes.intentos_por_etapa.TRANSCRIBIR.ciclo_1 -ne 1){throw 'Intento previo no persistido.'}
    # Acelera solo la expiración temporal; el contador/etapa persistidos proceden del claim real.
    $null=Sql "UPDATE respuestas_video SET procesamiento_contexto=JSON_SET(procesamiento_contexto,'$.bloqueo_hasta_utc','2020-01-01T00:00:00Z') WHERE id_respuesta=$taskId"
    Control @{}
    $taskSegundo=Iniciar 'jvm-despues'
    $completo=$false
    for($i=0;$i -lt 60;$i++){
        $despues=Sql "SELECT procesamiento_contexto FROM respuestas_video WHERE id_respuesta=$taskId" | ConvertFrom-Json
        if($despues.etapa_actual -eq 'EVALUAR'){$completo=$true;break}
        Start-Sleep -Milliseconds 500
    }
    if(-not $completo){throw 'No se reanudó hasta métricas.'}
    if($despues.intentos_por_etapa.TRANSCRIBIR.ciclo_1 -ne 2 -or $despues.intentos_por_etapa.EXTRAER_AUDIO.ciclo_1 -ne 1){throw 'Repitió etapa o perdió contador.'}
    $t=Sql "SELECT COUNT(*) FROM transcripciones WHERE id_respuesta=$taskId AND confianza_stt IS NULL"
    $m=Sql "SELECT COUNT(*) FROM metricas_comunicacion WHERE id_respuesta=$taskId AND version_metricas='MET-1.0'"
    if($t -ne '1' -or $m -ne '1'){throw 'Resultado no único.'}
    $audit=Invoke-RestMethod 'http://127.0.0.1:19090/__audit'
    $clave="/usuarios/$($registro.id_usuario)/sesiones/$($sesion.id_sesion)/preguntas/$($preguntas[0].id_pregunta)/audio.wav"
    $puts=@($audit.peticiones|Where-Object {$_.metodo -eq 'PUT' -and $_.clave -eq "/ep360-restart-local$clave"}).Count
    if($puts -ne 1){throw 'Extracción/subida de audio repetida.'}
    $evidencia=@{correcto=$true;fecha_utc=[datetime]::UtcNow.ToString('o');mysql=(Sql 'SELECT VERSION()');jvm_antes=$taskPrimero.Id;jvm_despues=$taskSegundo.Id;terminacion_forzada_durante_stt=$true;lease_expirado_por_sql_para_acelerar=$true;contexto_antes=$antes;contexto_despues=$despues;puts_audio=$puts;transcripciones=1;metricas=1;id_respuesta=$taskId}
    $evidencia|ConvertTo-Json -Depth 15|Set-Content -LiteralPath 'docs/evidencias/recuperacion-jvm-incremento-6.json' -Encoding utf8
    Write-Output 'PASS: JVM terminada en STT; nueva JVM recupera lease expirado, conserva audio, contabiliza intento 2 y termina MET-1.0.'
} finally {
    foreach($p in @($taskPrimero,$taskSegundo)){if($null -ne $p -and -not $p.HasExited){Stop-Process -Id $p.Id -ErrorAction SilentlyContinue}}
    Control @{}
}

