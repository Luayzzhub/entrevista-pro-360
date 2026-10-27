"""Valida evidencia ejecutada, inventaría versiones y consolida la entrega sin staging."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json
import re
import subprocess
import sys
import os
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / '.tools'
OUT = ROOT / 'docs/evidencias'
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
MENSAJE = 'feat: implementar historial paginado D-10, comparacion evolutiva PM-17 y vista MCK10'
LIMITES = 'Verificación local con clientes S3/HTTP y binarios reales sobre fixtures S3/STT/LLM. No certifica bucket/IAM AWS, cuenta/modelos OpenAI de producción ni matriz de hardware físico.'

def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True, encoding='utf-8').strip()

def leer(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def guardar(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def suites(motor):
    resultado = []
    for p in sorted((TOOLS / ('inc10-'+motor+'-reports')).glob('TEST-*.xml')):
        a = ET.parse(p).getroot().attrib
        resultado.append(dict(nombre=a['name'], descubiertas=int(a['tests']), fallas=int(a['failures']), errores=int(a['errors']), omitidas=int(a['skipped']), sha256=sha(p)))
    assert resultado, motor
    totales = {k: sum(s[k] for s in resultado) for k in ['descubiertas','fallas','errores','omitidas']}
    assert totales['descubiertas'] == 194 and totales['fallas'] == totales['errores'] == 0, (motor,totales)
    assert totales['omitidas'] == (2 if motor == 'h2' else 0)
    texto = (TOOLS/('inc10-'+motor+'.log')).read_text(encoding='utf-8', errors='replace')
    assert 'BUILD SUCCESS' in texto[-1500:]
    resumen = re.findall(r'Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)', texto)[-1]
    assert list(map(int,resumen)) == [totales[k] for k in ['descubiertas','fallas','errores','omitidas']]
    return {**totales, 'ejecutadas':totales['descubiertas']-totales['omitidas'], 'suites':resultado, 'log':'.tools/inc10-'+motor+'.log'}

# RF, función, incrementos, domain, application, infrastructure, presentation/Angular,
# pruebas de backend y escenario Chrome. Los RF de captura no llaman al backend antes de confirmar.
TRAZA = [
('01','Registro',[2],'Usuario','RegistrarUsuarioService','UsuarioJpaAdapter; BCryptPasswordHasher','AuthController; RegistroComponent','UsuarioTest; AuthPerfilIntegrationTest','Registro MCK02'),
('02','Acceso y cierre',[2],'Usuario','AutenticarUsuarioService','JwtTokenProvider; JwtAuthenticationFilter','AuthController; AuthService; LoginComponent; authGuard','IdentidadConfigTest; AuthPerfilIntegrationTest','Inicio MCK01; cierre y rutas protegidas'),
('03','Perfil',[2],'Usuario','GestionarPerfilService','UsuarioJpaAdapter','PerfilController; PerfilComponent','UsuarioTest; AuthPerfilIntegrationTest','Perfil: nombre/resumen persistidos y correo readonly'),
('04','Catálogo',[1,3],'Puesto; Pregunta','ConsultarCatalogoService','CatalogoJpaAdapter','CatalogoController; CatalogoComponent; PreguntasComponent','PuestoPreguntaTest; CatalogoIntegrationTest','MCK04: 3 puestos y 9 preguntas'),
('05','Inicio de práctica',[3],'SesionEntrevista','GestionarSesionService','SesionJpaAdapter; UsuarioJpaRepository','SesionController; PanelComponent; IniciarPracticaComponent','SesionEntrevistaTest; SesionIntegrationTest','MCK03: sesión activa única, conflicto409 y continuar'),
('06','Tres preguntas secuenciales',[3,8],'SesionEntrevista; Pregunta','GestionarSesionService; ProcesarRespuestasService','SesionJpaAdapter; ProcesamientoJpaAdapter','SesionController; SesionComponent','SesionEntrevistaTest; SesionIntegrationTest; EvaluacionIntegrationTest','P1→P2→P3 capturadas en Chrome y cierre'),
('07','Permisos',[4],'No invocado en permisos locales','No invocada antes de confirmar','Web API MediaDevices en navegador','CapturaRespuestaComponent; GrabacionMediaService','ConfirmacionRespuestaIntegrationTest (frontera al confirmar)','MCK05: ambas pistas live/enabled y aviso de privacidad'),
('08','Captura',[4,5],'RespuestaVideo (al confirmar)','GestionarRespuestaService (al confirmar)','FfprobeInspectorMultimediaAdapter (al confirmar)','CapturaRespuestaComponent; GrabacionMediaService','RespuestaVideoTest; FfprobeInspectorMultimediaAdapterTest','MCK06: MediaRecorder real en 3 respuestas'),
('09','Límite multimedia',[4,5],'RespuestaVideo','GestionarRespuestaService','FfprobeInspectorMultimediaAdapter','RespuestaController; GrabacionMediaService','RespuestaVideoTest; FfprobeInspectorMultimediaAdapterTest; ConfirmacionRespuestaIntegrationTest','Temporizador 00:02/01:00 y duración real1..60; test:media para borde60s'),
('10','Revisión y repetición',[4,5],'Sin mutación durante revisión','GestionarRespuestaService al confirmar','S3AlmacenamientoVideoAdapter al confirmar','CapturaRespuestaComponent; GrabacionMediaService','ConfirmacionRespuestaIntegrationTest','MCK07: reproducción y repetición sin nuevas respuestas'),
('11','Subida privada',[5],'RespuestaVideo','GestionarRespuestaService','S3AlmacenamientoVideoAdapter','RespuestaController; RespuestaService','S3AlmacenamientoVideoAdapterTest; ConfirmacionRespuestaIntegrationTest','PUT real SDK/S3 local, datos privados y reproducción firmada'),
('12','Metadatos',[5],'RespuestaVideo; ContextoProcesamiento','GestionarRespuestaService','RespuestaVideoJpaEntity; ProcesamientoJpaAdapter; ffprobe','RespuestaController; ProcesamientoRespuestaComponent','RespuestaVideoTest; ConfirmacionRespuestaIntegrationTest','MCK08: bytes, duración y PROC-1.0 persistidos'),
('13','Transcripción',[6],'Transcripcion','ProcesarRespuestasService','FfmpegExtractorAudioAdapter; OpenAiTranscripcionAdapter','RespuestaController; ResultadoRespuestaComponent','FfmpegExtractorAudioAdapterTest; OpenAiTranscripcionAdapterTest; PipelineIntegrationTest','WAV pcm_s16le/mono16k y transcripción visible'),
('14','Métricas',[6],'MetricaComunicacion; CalculadorMetricas','ProcesarRespuestasService','ProcesamientoJpaAdapter','RespuestaController; ResultadoRespuestaComponent','CalculadorMetricasTest; PipelineIntegrationTest','MET-1.0: conteos/PPM/muletillas visibles'),
('15','Evaluación',[7],'EvaluacionSTAR; ValidadorEvaluacion','ProcesarRespuestasService','OpenAiEvaluacionAdapter; EvaluacionJpaEntity','RespuestaController; ResultadoRespuestaComponent','ValidadorEvaluacionTest; OpenAiEvaluacionAdapterTest; EvaluacionIntegrationTest','COM-1.0: 4 criterios/justificaciones; STAR solo P2'),
('16','Feedback y resumen',[7,8],'SesionEntrevista; ResultadoRespuestaSesion','ProcesarRespuestasService; GestionarSesionService','ProcesamientoJpaAdapter; SesionJpaAdapter','SesionController; ResumenSesionComponent; ResultadoRespuestaComponent','SesionEntrevistaTest; EvaluacionIntegrationTest','COMPLETADA, medias87.50/75.00/79.17/83.33 y resumen'),
('17','Consulta propia',[3,7,8,9],'RegistroHistorial','GestionarSesionService; GestionarRespuestaService','SesionJpaAdapter; ProcesamientoJpaAdapter','SesionController; RespuestaController; HistorialComponent','HistorialIntegrationTest; EvaluacionIntegrationTest','Otra cuenta404/lista vacía; historial→resumen→MCK09'),
('18','Historial',[9],'RegistroHistorial; FiltroHistorial','GestionarSesionService','SesionJpaAdapter; ProcesamientoJpaAdapter','SesionController; HistorialComponent','CalculadoraEvolucionHistorialTest; HistorialIntegrationTest','5 filtros, fecha Lima→UTC, D10 y paginación real'),
('19','Comparación',[9],'CalculadoraEvolucionHistorial; ComparacionHistorial','GestionarSesionService','ProcesamientoJpaAdapter (lecturas por lotes)','SesionController; HistorialComponent','CalculadoraEvolucionHistorialTest; HistorialIntegrationTest','PM17 exacto81.25/87.50/-4.17/+4.16; móvil sin overflow'),
('20','Errores y recuperación',[5,6,7,8],'ContextoProcesamiento; SesionEntrevista','ProcesarRespuestasService; GestionarRespuestaService','WorkerRespuestas; RenovacionLeaseAdapter; ProcesamientoJpaAdapter','RespuestaController; ProcesamientoRespuestaComponent; CancelarPracticaComponent','ContextoProcesamientoTest; PipelineIntegrationTest; EvaluacionIntegrationTest','3 fallos STT, manual único, recuperación y CANCELADA sin promedio')
]

def versiones():
    # Resolver de nuevo el POM efectivo; no se modifica el manifiesto de dependencias.
    mvn = str(TOOLS/'apache-maven-3.9.9/bin/mvn.cmd').replace("'", "''")
    command = f"& '{mvn}' '-Dmaven.repo.local=../.tools/m2' '-B' '-ntp' 'help:effective-pom' '-Doutput=../.tools/inc10-effective-pom.xml'; exit $LASTEXITCODE"
    entorno=os.environ.copy()
    entorno.setdefault('JAVA_HOME','C:/Program Files/Java/jdk-17')
    entorno['PATH']=str(Path(entorno['JAVA_HOME'])/'bin')+os.pathsep+entorno.get('PATH','')
    p = subprocess.run(['powershell.exe','-NoProfile','-Command',command], cwd=ROOT/'backend', env=entorno, capture_output=True, text=True, encoding='utf-8', errors='replace')
    (TOOLS/'inc10-versiones.log').write_text(p.stdout+p.stderr,encoding='utf-8')
    assert p.returncode == 0, 'No se resolvió el POM efectivo'
    pom = ET.parse(TOOLS/'inc10-effective-pom.xml').getroot()
    deps=[]
    consumidores={
        'spring-boot-starter-web':'Los 5 controladores REST; DTO públicos',
        'spring-boot-starter-validation':'DTO registro/perfil/sesión y parámetros de controladores',
        'spring-boot-starter-data-jpa':'8 entidades/repositorios; SesionJpaAdapter y ProcesamientoJpaAdapter',
        'spring-boot-starter-security':'SecurityConfig; BCryptPasswordHasher; JwtAuthenticationFilter',
        'spring-security-oauth2-jose':'JwtTokenProvider; IdentidadConfig',
        'mysql-connector-j':'Datasource MySQL 8.4.13; JAR e integración',
        'flyway-core':'V1/V2 y validación de checksums', 'flyway-mysql':'Mismas V1/V2 en MySQL',
        's3':'S3AlmacenamientoVideoAdapter; firma PA-04',
        'spring-boot-starter-test':'23 suites de dominio/arquitectura/adaptadores/presentación',
        'h2':'Integración efímera con mismas V1/V2'}
    for d in pom.find('m:dependencies',NS):
        item={k:d.findtext('m:'+k,namespaces=NS) for k in ['groupId','artifactId','version']}
        assert item['version'] and '${' not in item['version']
        item['scope']=d.findtext('m:scope','compile',NS);item['uso']=consumidores[item['artifactId']];deps.append(item)
    package=leer(ROOT/'frontend/package.json');lock=leer(ROOT/'frontend/package-lock.json')
    usos={'@angular/core':'Signals, DI; HistorialComponent, ResumenSesionComponent, ResultadoRespuestaComponent y resto de vistas',
          '@angular/common':'HttpClient, DatePipe/DecimalPipe en historial/resumen/resultados',
          '@angular/forms':'ReactiveFormsModule en identidad/perfil; FormsModule en HistorialComponent',
          '@angular/router':'12 interfaces, authGuard y navegación historial→resumen→resultado',
          '@angular/compiler':'Plantillas de todos los componentes', '@angular/platform-browser':'bootstrapApplication en main.ts',
          'rxjs':'switchMap/toSignal, sondeo y cancelación; HistorialComponent y ResumenSesionComponent',
          'tslib':'Helpers de TypeScript', '@angular/build':'Build de producción AOT', '@angular/cli':'Comandos ng build/serve',
          '@angular/compiler-cli':'AOT y validación de plantillas', 'typescript':'Comprobación estricta de tipos y tests de medios'}
    front=[]
    for categoria in ['dependencies','devDependencies']:
        for nombre,version in package[categoria].items():
            instalada=leer(ROOT/'frontend/node_modules'/nombre/'package.json')['version']
            assert version==lock['packages']['node_modules/'+nombre]['version']==instalada
            front.append(dict(nombre=nombre,version=version,instalada=instalada,categoria=categoria,uso=usos[nombre]))
    with zipfile.ZipFile(ROOT/'backend/target/entrevista-pro-360-0.1.0-SNAPSHOT.jar') as z:
        libs=sorted(n.removeprefix('BOOT-INF/lib/') for n in z.namelist() if n.startswith('BOOT-INF/lib/') and n.endswith('.jar'))
    binarios={}
    for nombre,exe in [('ffmpeg',os.environ.get('FFMPEG_BIN',str(TOOLS/'ffmpeg/ffmpeg.exe'))),('ffprobe',os.environ.get('FFPROBE_BIN',str(TOOLS/'ffmpeg/ffprobe.exe')))]:
        linea=subprocess.check_output([exe,'-version'],text=True,encoding='utf-8').splitlines()[0]
        assert '9.0.2' in linea;binarios[nombre]=linea
    return dict(fecha_utc=datetime.now(timezone.utc).isoformat(),alcance='Incrementos 1 a 10',backend=deps,frontend={d['nombre']:d['version'] for d in front},frontend_detalle=front,
        librerias_runtime_jar=libs,fuente_backend='POM efectivo recién resuelto y BOOT-INF/lib del JAR ejecutado',fuente_frontend='package.json + package-lock.json + node_modules (igualdad comprobada)',
        java_puro='CalculadoraEvolucionHistorial, FiltroHistorial, CalculadorMetricas y ValidadorEvaluacion: JDK17, sin dependencias externas',
        herramientas=dict(chrome=leer(OUT/'pruebas-browser-incremento-10.json')['navegador'],playwright=leer(TOOLS/'browser-check/node_modules/playwright/package.json')['version'],mysql='8.4.13',**binarios,maven='3.9.9',python=sys.version.split()[0],node=subprocess.check_output(['node','--version'],text=True).strip()))

def main():
    run=leer(TOOLS/'inc10-ejecucion.json');assert run['correcto']
    head=git('rev-parse','HEAD');assert head==git('rev-parse','origin/main')==run['head']
    assert git('log','-1','--format=%s')==MENSAJE and git('branch','--show-current')=='main'
    remoto=git('ls-remote','origin','refs/heads/main').split()[0];assert remoto==head
    assert not git('diff','--cached','--name-only') and not git('ls-files','.tools')
    assert git('check-ignore','.tools/inc10-ejecucion.json')
    invariantes=['backend/pom.xml','frontend/package.json','frontend/package-lock.json','backend/src/main/resources/db/migration']
    assert not git('diff','HEAD','--name-only','--',*invariantes)
    assert not git('diff','c626aaf','--name-only','--','backend/src/main/resources/db/migration')
    assert sorted(p.name for p in (ROOT/'backend/src/main/resources/db/migration').glob('*.sql'))==['V1__esquema_inicial.sql','V2__catalogo_semilla.sql']
    h2=suites('h2');mysql=suites('mysql')
    browser=leer(OUT/'pruebas-browser-incremento-10.json')
    assert browser['correcto'] and len(browser['RF'])==20 and len(browser['endpoints'])==14 and len(browser['interfaces'])==12
    assert run['inicio_utc'] <= browser['fecha_utc'] <= run['fin_utc'], 'Evidencia Chrome ajena a la ejecución actual'
    assert not browser['errores_javascript'] and not browser['errores_consola']
    build=(TOOLS/'inc10-angular-build.log').read_text(encoding='utf-8',errors='replace')
    assert 'Application bundle generation complete' in build and not re.search(r'WARNING|ERROR',build)
    media=(TOOLS/'inc10-media.log').read_text(encoding='utf-8',errors='replace');assert 'pass 6' in media and 'fail 0' in media
    schema=(TOOLS/'inc10-schema.txt').read_text(encoding='utf-8');assert '8.4.13' in schema
    columnas={k:int(v) for k,v in re.findall(r'^(evaluaciones_star|metricas_comunicacion|preguntas|puestos|respuestas_video|sesiones_entrevista|transcripciones|usuarios)\t(\d+)\s*$',schema,re.M)}
    assert len(columnas)==8 and sum(columnas.values())==68
    assert re.search(r'^1\t1\s*$',schema,re.M) and re.search(r'^2\t1\s*$',schema,re.M)
    commits={i:git('rev-parse',ref) for i,ref in enumerate(['c626aaf','e47eeb6','214c011','dd40f77','4d00e56','f702fc4','ca798f5','a05ca28',head],1)}
    for commit in commits.values():subprocess.run(['git','merge-base','--is-ancestor',commit,'origin/main'],cwd=ROOT,check=True)
    versions=versiones();guardar(OUT/'versiones-resueltas.json',versions)
    archivos=['backend/target/entrevista-pro-360-0.1.0-SNAPSHOT.jar','frontend/dist/entrevista-pro-360/browser/index.html',*invariantes[:3],*[str(p.relative_to(ROOT)).replace('\\','/') for p in (ROOT/invariantes[3]).glob('*.sql')], 'scripts/pruebas/verificar-incremento-10-entregable.mjs','scripts/pruebas/recorrido-incremento10.mjs','scripts/pruebas/registrar-evidencias-incremento10.py','scripts/pruebas/servidor-entregable.mjs','scripts/pruebas/pipeline-resumen-http-local.cjs']
    assets={str(p.relative_to(ROOT)).replace('\\','/'):sha(p) for p in sorted((ROOT/'frontend/dist/entrevista-pro-360/browser').rglob('*')) if p.is_file()}
    resultados=[dict(RF='RF-'+n,funcion=f,estado='Verificado localmente',incrementos=incs,commits=[commits[i] for i in incs],domain=d,application=a,infrastructure=infra,presentation_angular=ui,pruebas_h2_mysql=tests,chrome=e2e) for n,f,incs,d,a,infra,ui,tests,e2e in TRAZA]
    evidencia=dict(incremento=10,fecha_utc=datetime.now(timezone.utc).isoformat(),estado='implementado_verificado_localmente_y_aprobado_en_auditoria_final',resultado='IMPLEMENTADO_VERIFICADO_LOCALMENTE_Y_APROBADO_EN_AUDITORIA_FINAL',
        git=dict(rama='main',head=head,origin_main=remoto,incremento9='AUDITADO_APROBADO_SIN_OBSERVACIONES',mensaje_incremento9=MENSAJE,commits_publicados=commits,incremento10_sin_commit=True,indice_vacio=True,tools_ignorado=True,nota_corte='Los campos Git describen la ejecucion local previa al cierre documental, commit y push finales autorizados tras la auditoria'),
        backend_h2=h2,backend_mysql=mysql,frontend=dict(build='AOT producción sin advertencias',medios=6,chrome_casos=browser['total_casos'],interfaces=browser['interfaces'],endpoints=browser['endpoints'],errores_javascript=0,errores_consola=0),
        esquema=dict(version='8.4.13',tablas_negocio=8,columnas=68,por_tabla=columnas,migraciones=['V1','V2'],ddl_auto='validate'),
        trazabilidad=resultados,ejecucion=run,hashes_sha256={f:sha(ROOT/f) for f in archivos},angular_assets_sha256=assets,
        evidencia_browser_sha256=sha(OUT/'pruebas-browser-incremento-10.json'),versiones_sha256=sha(OUT/'versiones-resueltas.json'),limites=LIMITES)
    guardar(OUT/'pruebas-incremento-10.json',evidencia)
    publicar_documentos(evidencia,browser)
    print(f'Evidencia validada: H2 {h2["ejecutadas"]}/194, MySQL194/194, medios6, Chrome{browser["total_casos"]}, 20RF/14endpoints/12interfaces, esquema8/68, índice vacío.')

def publicar_documentos(e,b):
    commits=e['git']['commits_publicados'];todos=', '.join('`'+c[:7]+'`' for c in commits.values())
    intro=f'''# Matriz de evidencias — Incrementos 1 a 10 (Tercer Entregable)

Fecha: 27/10/2026, America/Lima. **20/20 RF Verificados localmente**. Incremento 9 **AUDITADO Y APROBADO SIN OBSERVACIONES por el Orquestador**; RF-17/18/19 Verificados. Incremento 10 implementado, verificado localmente y **auditado y aprobado sin observaciones por el Orquestador**. Evidencia nueva en `evidencias/pruebas-incremento-10.json` y `evidencias/pruebas-browser-incremento-10.json`.

## Cuatro criterios del tercer entregable

| Criterio | Commits publicados en origin/main (Incrementos 1 a 9) | Archivos revisables | Pruebas ejecutadas | Estado |
| --- | --- | --- | --- | --- |
| Diseño y desarrollo de la arquitectura inicial | {todos} | arquitectura.md; backend/src/main/java/pe/utp/entrevistapro360/; V1/V2 | LayerDependenciesTest; 192 H2 y194 MySQL; consulta esquema8/68 | Verificado localmente |
| Identificar y explicar las librerías utilizadas | {todos} | dependencias.md; evidencias/versiones-resueltas.json; POM/lock intactos | POM efectivo recién resuelto; igualdad package/lock/node_modules; inventario del JAR | Verificado localmente |
| Git y GitHub con evidencias de avances y actualizaciones | {todos} | evidencias/pruebas-incremento-10.json: git; informes1..9 | merge-base de los9 commits; HEAD=origin/main=ls-remote; índice vacío | Auditado y aprobado (cierre del tercer entregable en main) |
| Interfaces gráficas que cubran el alcance comprometido | {todos} | frontend/src/app/features/; recorrido-incremento10.mjs; evidencias/pruebas-browser-incremento-10.json | Chrome:12 interfaces,14 endpoints,20 RF; {b['total_casos']} casos; cero errores JS/consola; AOT y medios6 | Verificado localmente |

## Trazabilidad funcional completa

Las clases backend se ubican bajo `backend/src/main/java/pe/utp/entrevistapro360/` en la capa indicada; las suites bajo `backend/src/test/java/pe/utp/entrevistapro360/`. Componentes Angular bajo `frontend/src/app/`. Cada suite indicada se ejecutó en **ambos motores** salvo las dos pruebas de `MySqlSchemaIntegrationTest`, exclusivas de MySQL. La captura local no invoca capas backend hasta confirmar; se explicita esa frontera para no atribuir comportamiento inexistente. Los escenarios Chrome pertenecen a la ejecución nueva de `scripts/pruebas/recorrido-incremento10.mjs`; los informes anteriores añaden pruebas de bordes, concurrencia, firmas y fallos.

| RF / función / estado | Incrementos y commits reales en origin/main | domain | application | infrastructure | presentation / Angular | Unitarias e integración H2/MySQL | Escenario Chrome / resultado observado | Pendiente externo |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
'''
    for r in e['trazabilidad']:
        cs=', '.join(f'{i}:`{commits[i][:7]}`' for i in r['incrementos'])
        pendiente='AWS bucket/IAM' if r['RF']=='RF-11' else 'OpenAI cuenta/modelo producción' if r['RF'] in ['RF-13','RF-15'] else 'Hardware físico' if r['RF'] in ['RF-07','RF-08'] else 'Ninguno funcional local; despliegue externo sin certificar'
        intro+=f"| {r['RF']} {r['funcion']} — **Verificado localmente** | {cs} | {r['domain']} | {r['application']} | {r['infrastructure']} | {r['presentation_angular']} | {r['pruebas_h2_mysql']} | {r['chrome']} | {pendiente} |\n"
    intro+='\n## Alcance de aceptación\n\n'+LIMITES+' Las fechas del filtro usan America/Lima; la API devuelve UTC. PM-17 se calcula sobre sesiones completas filtradas antes de paginar. Los9 commits citados corresponden al corte de ejecución previo al cierre final. El Incremento10 fue auditado y aprobado sin observaciones por el Orquestador y se identifica en main por el mensaje exacto `feat: consolidar integracion final E2E, matriz 20/20 RF y evidencias del tercer entregable`; su hash final se consulta con git log para evitar una referencia circular dentro del propio commit.\n'
    intro+='\n## Cierre del incremento final\n\n| Incremento | Estado | Referencia Git |\n| --- | --- | --- |\n| Incremento 10 | Auditado y aprobado (cierre del tercer entregable en main) | main; mensaje exacto de cierre indicado arriba |\n'
    (ROOT/'docs/matriz_evidencias_rf.md').write_text(intro,encoding='utf-8')
    informe=f'''# Verificación del Incremento 10 — Entrega Final del Tercer Entregable

Fecha: 27/10/2026, America/Lima.\n\n**Estado:** Implementado, verificado localmente y **aprobado en auditoría final del tercer entregable**.\n\nEl Orquestador declaró el Incremento 10 **AUDITADO Y APROBADO SIN OBSERVACIONES**. Cierre documental y publicación final autorizados en main.

## Cierre del Incremento 9 antes del Paso 2

El Orquestador lo declaró **AUDITADO Y APROBADO SIN OBSERVACIONES**, RF-17/18/19 Verificados y20/20 RF locales. Se actualizaron informe9, JSON9 y matriz, y se creó un único commit atómico en main: **{e['git']['head']}**, mensaje exacto `{MENSAJE}`. Push origin main: a05ca28→{e['git']['head'][:7]}. Antes del Paso2 `git status --short --branch` mostró exclusivamente `## main...origin/main`; `.tools/` ignorado y fuera del índice. Al consolidar, HEAD, origin/main y ls-remote coinciden. El Paso2 no utiliza git add/commit.

## Resultado integral observado

| Verificación nueva | Resultado |
| --- | --- |
| Maven test H2 | 194 descubiertas,192 ejecutadas,2 exclusivas MySQL omitidas;0 fallos/errores |
| Maven test perfil mysql | 194 ejecutadas,0 omitidas/fallos/errores; MySQL8.4.13:33307 |
| LayerDependenciesTest | Verde en ambos motores; domain/application puros |
| Maven package | JAR ejecutable compilado y utilizado por Chrome |
| Angular | Build AOT producción servido desde dist; sin advertencias |
| Medios | 6 pruebas correctas |
| Chrome for Testing {b['navegador']} | {b['total_casos']} casos,20RF,14 endpoints,12 interfaces;0 errores JavaScript y0 consola |
| Esquema real | 8 tablas/68 columnas; Flyway1/2 correctas; ddl-auto=validate |
| Proveedores locales | {b['llamadas_stt']} llamadas STT, {b['llamadas_llm']} LLM; SDK S3 real; WAV pcm_s16le mono16000Hz verificado con ffprobe |
| Invariantes Git | V1/V2 iguales a c626aaf; sin V3+; POM/package/lock intactos; índice vacío |

El recorrido registra desde MCK02, inicia en MCK01, persiste perfil y recorre los3 puestos y sus9 preguntas. Comprueba sesión activa única con409 desde cliente HTTP sin producir errores de consola. Chrome usa dispositivos emulados y MediaRecorder nativo para grabar P1/P2/P3, temporizador real, repetición de P1 sin mutar BD y revisión/reproducción local. Cada respuesta atraviesa S3/ffprobe y las4 etapas persistidas, observadas en orden; MCK08 muestra los hitos y MCK09 los4 criterios con justificaciones, STAR solo en P2, MET-1.0, transcripción y video reproducible con SigV4 renovada y no-store. No se intercepta la API.

P3 cierra la primera sesión con87.50; otras3 sesiones reales por API comprueban medias75.00,79.17,83.33 (BigDecimal HALF_UP escala2) y completan la serie PM-17. Una quinta sesión graba P1, provoca3 fallos STT en P2, recupera con el único reintento manual y cancela conservando las2 evaluaciones, puntaje_promedio=null. Se rechaza un nuevo reintento con409. Los errores esperados se comprueban mediante cliente HTTP sin ocultar mensajes de consola de Chrome. D-02 definitivo, fencing, concurrencia, bordes1/60s, fechas04:30Z/05:00Z y cero/una completa tienen cobertura en las suites H2/MySQL y evidencia histórica9; no se afirma que todos esos bordes se reproduzcan otra vez en este recorrido Chrome.

MCK10 aplica puesto/fechas Lima/estado/COM-1.0; calcula PM-17 **81.25 /87.50 /-4.17 /+4.16** antes de paginar size2, conserva indicadores en página2, muestra Sin promedio para CANCELADA y explica la intersección incompatible D-10. Navega a resumen y resultado propios; otra cuenta recibe404 para los recursos y lista vacía. Cerrar sesión limpia estado en memoria, vuelve a login y bloquea navegación atrás/directa a historial. Móvil390x844 sin desbordamiento de página.

## Reproducción desde la raíz en PowerShell

```powershell
node scripts/pruebas/verificar-incremento-10-entregable.mjs
# Revalidar/consolidar los artefactos ya ejecutados, sin relanzar los servicios:
python scripts/pruebas/registrar-evidencias-incremento10.py
```

Prerrequisitos locales existentes en `.tools/`: Maven3.9.9/cache m2, MySQL8.4.13 con datadir inicializado, ffmpeg/ffprobe9.0.2, Chrome for Testing y Playwright auxiliar1.64.1; node_modules instalado con lock. JDK17 en JAVA_HOME o ruta local predeterminada. El runner usa bases aisladas ep360_incremento10_test y ep360_incremento10_browser; no elimina bases ni modifica cuentas de producción. Usa root local sin contraseña solo para este datadir de pruebas. Reutiliza MySQL33307 si ya está vivo y valida su versión; si lo inicia, lo apaga al terminar. Rechaza puertos ocupados18080/14200/19090/19091 y cierra únicamente procesos propios, ocultos. No instala dependencias ni toca POM/package/lock. EP360_CHROME_BIN y EP360_PYTHON_BIN permiten seleccionar ejecutables auxiliares.

El entorno restringido de Codex requirió ejecutar fuera del sandbox por bloqueo TCP10013; el primer intento interrumpido no constituye evidencia de pruebas. La ejecución final correcta queda en `.tools/inc10-ejecucion.json`, con tiempos/códigos/logs. En otros equipos hay que preparar los prerrequisitos: `.tools/` se excluye de Git por diseño. La base browser conserva sus datos propios para revisión; los correos únicos permiten volver a ejecutar. Los tests mysql usan otra base exclusiva.

## Paquete auditable

- `docs/evidencias/pruebas-incremento-10.json`: suites separadas por motor, trazabilidad20RF, esquema,9 commits completos, hashes de JAR/scripts/invariantes y todos los assets AOT.
- `docs/evidencias/pruebas-browser-incremento-10.json`: casos,12 interfaces,14 endpoints, tráfico real, etapas, sesiones y auditorías locales.
- `docs/evidencias/versiones-resueltas.json`: POM efectivo nuevo, librerías exactas del JAR y12 paquetes frontend comprobados contra lock/instalación.
- `docs/matriz_evidencias_rf.md`, `docs/arquitectura.md`, `docs/dependencias.md`, `README.md`: cierre de los4 criterios y alcance.
- `.tools/inc10-{{h2,mysql,package,media,angular-build,browser,versiones}}.log`, `inc10-h2-reports/`, `inc10-mysql-reports/`, `inc10-schema.txt`: logs/XML originales locales ignorados; cada XML tiene hash en el JSON.
- `.tools/inc10-resumen.png`, `inc10-historial.png`, `inc10-historial-movil.png`: capturas del build ejecutado.

## Límites técnicos

{LIMITES} La firma GET se verifica criptográficamente por la fixture local y la política privada se prueba con clientes reales; no acredita permisos de una cuenta AWS. STT/LLM devuelven resultados controlados del fixture. La integración de producción sigue requiriendo configuración y validación externa. El Orquestador aprobó sin observaciones la auditoría final del Incremento10 y del tercer entregable.
'''
    (ROOT/'docs/verificacion_incremento_10.md').write_text(informe,encoding='utf-8')

if __name__=='__main__':
    main()
