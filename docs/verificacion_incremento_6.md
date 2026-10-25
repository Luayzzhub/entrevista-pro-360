# Verificación del Incremento 6

Fecha: 25/10/2026, America/Lima. Estado: **AUDITADO Y APROBADO SIN OBSERVACIONES; cerrado y publicado mediante este commit atómico en main**. Las evidencias de ejecución conservan su fecha y alcance.

## Cierre previo del Incremento 5

Se actualizaron README, matriz RF, informe 5 y JSON 5 para registrar aprobación sin observaciones. Se creó en main el commit atómico **4d00e56b0f905f1a2b0770453b913538bd9519c8**, con el mensaje exacto:

```text
feat: implementar confirmacion idempotente de respuesta, validacion ffprobe, subida S3 y progreso MCK08
```

El push a origin/main del Incremento 5 terminó correctamente (dd40f77..4d00e56); ls-remote coincidió con el hash. Antes de escribir código del Incremento 6, git status --short --branch mostró únicamente `## main...origin/main`. Durante la verificación del 6, HEAD era 4d00e56 y el índice permanecía vacío.

## Resultado funcional

El worker procesa EXTRAER_AUDIO → TRANSCRIBIR → METRICAS y termina con TRANSCRITO, último hito TRANSCRITO y etapa EVALUAR. Error, próximo intento y lease quedan nulos. Nunca reclama EVALUAR, llama al LLM, persiste evaluación ni completa la sesión.

- RF-13: ffmpeg real genera WAV PCM s16le mono 16000 Hz; HttpClient envía el multipart oficial con snapshot exacto gpt-4o-mini-transcribe-2025-12-15, idioma es y formato json. Texto trim, confianza_stt NULL, idioma es y fecha UTC persistidos.
- RF-14: CalculadorMetricas reside en dominio puro; NFKC, tokens Unicode con guion/apóstrofo interno, BigDecimal HALF_UP y catálogo exacto de siete muletillas. La locución o sea admite solo espacios entre tokens; detalle omite ceros y su suma coincide con el total.
- RF-20: etapas 1–3 y reintento implementados; sigue En progreso por evaluación y aceptación completa posteriores.
- API: GET calcula elegibilidad real y entrega métricas seguras; POST /api/v1/respuestas/{id}/reintento devuelve 202, 409 REINTENTO_NO_DISPONIBLE o 404 indistinguible para ajena/inexistente.
- MCK 08 muestra avance, ofrece Reintentar análisis una vez, anuncia CANCELADA ante falla definitiva y detiene el polling en TRANSCRITO/EVALUAR o terminales.

## Durabilidad y transacciones

Planificador fixedDelay de 2 s, configurable, con invocación secuencial y AtomicBoolean. Heartbeat en un hilo independiente cada 20 s. Lease de 90 s. El claim confirma versión +1 e intento +1 antes de la operación externa. Los locks cortos siguen el orden sesión/respuesta; ffmpeg y HTTP quedan fuera de transacciones.

Las actualizaciones finales verifican versión, etapa y lease bajo bloqueo. Una cancelación concurrente conserva la sesión terminal y libera únicamente el lease. El callback tardío de heartbeat no puede renovar un trabajo terminado. Se pagina la búsqueda de candidatos y se excluye EVALUAR.

Timeout/I/O/408/429/5xx: máximo tres ejecuciones por etapa y ciclo, backoff max(5 s, Retry-After) y max(15 s, Retry-After). El tercer fallo del ciclo 1 habilita un manual; ciclo 2 agotado o falla no recuperable cancela conforme a D-02, con fecha_fin >= fecha_inicio y promedio null. Un tercer claim interrumpido se agota al expirar sin cuarta ejecución.

El cliente STT limita también el tiempo de lectura del cuerpo HTTP completo a 60 s y su memoria a 2 MB. Se verificó un proveedor que envía cabeceras y luego demora el cuerpo. Los archivos temporales se eliminan en finally. S3 usa WAV estable privado y una sola solicitud por intento del SDK; PROC-1.0 gobierna los reintentos.

## Pruebas ejecutadas

| Comprobación | Resultado final |
| --- | --- |
| Maven H2, mismas V1/V2 | 132 descubiertas, **130 ejecutadas**, 2 específicas de MySQL omitidas, 0 fallas/errores |
| Maven MySQL **8.4.13** | **132 ejecutadas**, 0 omitidas/fallas/errores |
| Dominio nuevo MET/PROC | 7 pruebas MET + 4 PROC; Unicode, redondeo, muletillas, límites de lease, ciclos, backoff, rechazo de cuarto intento |
| Adaptadores nuevos | 2 ffmpeg reales + 5 STT HTTP: multipart/privacidad, 200, 408/429/503, rechazo 400/401/403/422, blanco/nulo, cuerpo detenido y límite de memoria |
| Pipeline en H2 y MySQL | 11 escenarios por motor: flujo, idempotencia, lease/reinicio, heartbeat/fencing, claims concurrentes, manual/ciclo 2, cancelación durante STT, blanco y cero palabras |
| npm run test:media | 6 aprobadas |
| npm run build | Correcto, sin advertencias de presupuesto |
| mvn package -DskipTests | Correcto, JAR ejecutable |
| Chrome headless real | 3 escenarios MCK 08, desktop y móvil, 0 errores JavaScript; éxito, manual tras 3 fallos HTTP, blanco definitivo |
| Recuperación entre procesos JVM | Terminación real durante STT, segundo proceso recupera desde AUDIO_EXTRAIDO y completa METRICAS sin repetir audio |
| Esquema MySQL | 8 tablas de negocio, 68 columnas, V1/V2 intactas, sin V3+, Hibernate validate |

Chrome captura video con cámara y micrófono emulados y confirma por la API real; no intercepta ni sustituye las respuestas de la aplicación. Los dos escenarios exitosos terminan con métricas visibles y polling detenido. Se verifica 202 del manual, 409 al intentar otro manual, 404 con JWT válido de propietario ajeno y enlaces de salida ante CANCELADA. ffprobe comprueba tres WAV reales; STT recibe seis solicitudes con exactamente cuatro campos, modelo exacto y sin identificadores añadidos.

La prueba entre JVMs usa una base y bucket separados. Termina el primer PID cuando ya persistió el claim TRANSCRIBIR/intento 1 y el servidor local recibió la solicitud. **Solo la expiración temporal se acelera mediante SQL**, cambiando el lease a una fecha pasada; no se altera el contador/hito. El nuevo PID registra intento 2, deja una sola transcripción y métrica, mantiene EXTRAER_AUDIO.ciclo_1=1 y no repite el PUT de WAV. Las pruebas de dominio verifican separadamente el borde estricto de 90 s.

## Reproducción y aislamiento

Desde backend, con JDK 17 y binarios ffmpeg/ffprobe disponibles:

```powershell
mvn test
$env:TEST_DB_URL='jdbc:mysql://127.0.0.1:33307/ep360_incremento6_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:TEST_DB_USERNAME='root'
$env:TEST_DB_PASSWORD=''
mvn test '-Dspring.profiles.active=mysql'
mvn package -DskipTests
```

La base del ejemplo debe ser exclusivamente de pruebas. Quita TEST_DB_URL antes del pase H2. El perfil de test deshabilita el worker programado; las pruebas lo invocan con Clock controlable y HTTP S3/STT locales. Maven del entorno está en .tools/apache-maven-3.9.9; repositorio local .tools/m2. No se añadieron dependencias de aplicación.

Desde frontend: npm run test:media y npm run build. La verificación auxiliar requiere Chrome y Playwright en .tools/browser-check. Los scripts versionados son:

- scripts/pruebas/pipeline-http-local.cjs: endpoints S3 19090 y STT 19091, controles y auditoría locales.
- scripts/pruebas/mck08-incremento6.cjs: recorrido real contra Angular 14200 y backend 18080; genera el JSON del navegador y capturas en .tools.
- scripts/pruebas/recuperacion-jvm-incremento6.ps1: dos JVMs en 18081, MySQL aislado 33307 y prueba de interrupción.
- scripts/pruebas/registrar-evidencias-incremento6.py: comprueba logs, reportes, esquema y estado Git antes de consolidar el JSON.

Se usan tres bases separadas: ep360_incremento6_test para Maven, ep360_incremento6_browser para Chrome y ep360_incremento6_restart para interrupción. La última usa bucket local ep360-restart-local; el navegador ep360-pruebas-local. La verificación inicial detectó que una JVM de demostración sobre la base de tests podía reclamar sus filas; se detuvo, se separaron bases/buckets y la suite completa se repitió satisfactoriamente. También se corrigió el lector de auditoría multipart del fixture, que confundía filename con name; el cliente ya enviaba las cuatro partes correctas.

En la ejecución Chrome se aceleran únicamente intervalo a 500 ms y backoff a 1/1 s. Producción conserva 2000 ms y 5/15 s; las pruebas de dominio verifican sus valores y Retry-After con Clock controlado.

## Evidencias y límites

- [Consolidado](evidencias/pruebas-incremento-6.json): resultados, suites, configuración, esquema, estado Git y SHA-256 del JAR.
- [Chrome](evidencias/pruebas-browser-incremento-6.json): tres casos, peticiones y formato WAV.
- [Reinicio JVM](evidencias/recuperacion-jvm-incremento-6.json): PIDs distintos y contextos persistidos antes/después.
- Logs y capturas auxiliares en .tools/inc6-*, ignorados por Git.

Los clientes Java, JPA, SDK S3, ffmpeg y Chrome son reales; los proveedores S3 y STT son servidores HTTP locales de prueba. Esto acredita el contrato y comportamiento del pipeline, **no una cuenta OpenAI activa, disponibilidad del snapshot en esa cuenta ni IAM/políticas de un bucket AWS de producción**. La fixture S3 comprueba cabecera de firma y ausencia de acceso anónimo, no valida criptográficamente IAM. Chrome usa dispositivos emulados.

Flyway 11.7.2 recomienda actualizar al detectar MySQL 8.4 porque su versión declarada de soporte probado es anterior. Las migraciones y validate se ejecutaron correctamente en 8.4.13; no se modificó el BOM ni el esquema. La tabla canónica reservada para evaluación conserva su nombre evaluaciones_star; no se añadieron tablas sugeridas por nombres de futuras funcionalidades.

Contrato STT contrastado con la [referencia oficial de transcripciones](https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create) y la [ficha del modelo](https://developers.openai.com/api/docs/models/gpt-4o-mini-transcribe). Se preserva el snapshot solicitado; no se sustituye automáticamente.

El Incremento 6 fue auditado y aprobado sin observaciones y queda cerrado mediante este commit. Incremento 7: evaluación EVAL-1.0/COM-1.0, EVALUADO, reproducción PA-04 y resultado MCK 09. El cierre automático y promedio global corresponden al Incremento 8; historial completo sigue fuera de este alcance.

