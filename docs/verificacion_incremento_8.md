# Verificación del Incremento 8

Fecha: 27/10/2026, America/Lima. Estado: **AUDITADO Y APROBADO SIN OBSERVACIONES por el Orquestador**. RF-16 Verificado; RF-17 En progreso.

## Cierre publicado del Incremento 7

Antes de escribir código del Incremento 8 se actualizaron las referencias de auditoría del 7 en README, matriz RF, informe 7 y JSON 7. Se registró **AUDITADO Y APROBADO SIN OBSERVACIONES** y se publicó un único commit atómico en main:

- Hash: **ca798f5d112469ccac07668e53959cb1feaed314**.
- Mensaje exacto: `feat: implementar evaluacion EVAL-1.0 COM-1.0, reproduccion firmada PA-04 y resultado MCK09`.
- git push origin main: f702fc4..ca798f5 correcto.
- git ls-remote origin refs/heads/main: ca798f5d112469ccac07668e53959cb1feaed314.
- HEAD y origin/main coincidieron y git status --short --branch mostró únicamente `## main...origin/main`.
- .tools y artefactos temporales no entraron al índice.

El Incremento 8 se cierra mediante un único commit atómico en main y publicación en origin/main, con el mensaje: `feat: implementar cierre atomico de sesion COMPLETADA, promedio global y resumen de sesion`. Los resultados siguientes corresponden a la verificación previa a la auditoría.

## Resultado funcional

Se implementó el cierre completo de las tres respuestas, cálculo de promedio global y Resumen de sesión. RF-16 pasa a Verificado; RF-17 avanza con consulta propia de detalle/resumen, conservando historial integral y comparación fuera de este alcance.

- EVALUAR de P1/P2 mantiene EN_PROCESO y habilita la siguiente pregunta.
- Al persistir la tercera evaluación, la misma transacción valida las tres respuestas y completa la sesión si todos los datos son válidos.
- COMPLETADA publica puntaje_promedio y fecha_fin UTC; permite crear otra sesión inmediatamente.
- CANCELADA nunca se reabre ni recibe promedio. Un fallo definitivo de P3 conserva las evaluaciones P1/P2.
- Resumen de sesión: /sesiones/:id/resumen, protegido con authGuard y validación de ID entero positivo seguro.
- MCK09, detalle y MCK03 enlazan el resumen; una sesión cancelada muestra progreso y resultados parciales sin ceros inventados.

## Regla pura de dominio

ResultadoRespuestaSesion reúne RespuestaVideo, Transcripcion, MetricaComunicacion y EvaluacionSTAR. SesionEntrevista.calcularPromedioGlobal recibe ese conjunto y el banco de preguntas sin importar Spring, JPA, Jackson ni SDKs.

Solo completa una sesión EN_PROCESO con exactamente tres preguntas activas, distintas, del mismo puesto y órdenes 1/2/3. Exige tres identidades de respuesta distintas, vinculadas a la sesión/pregunta y con estado EVALUADO, transcripción no vacía, métrica MET-1.0 válida y evaluación que supera ValidadorEvaluacion.validarGuardada con el aplicaStar real.

La media suma los tres puntaje_general y divide entre tres mediante BigDecimal HALF_UP, escala 2:

| Puntajes individuales válidos | Promedio |
| --- | --- |
| 75.00, 81.25, 87.50 | **81.25** |
| 0.00, 0.00, 6.25 | **2.08** |
| 0.00, 0.00, 0.00 | **0.00** |

Con cero, una o dos respuestas, banco/identidades repetidas, respuesta no evaluada o cualquier bloque ausente/inválido, devuelve la sesión original EN_PROCESO sin promedio ni fecha final. Los ceros de la tabla son evaluaciones reales válidas; nunca sustituyen respuestas ausentes o fallidas.

COMPLETADA y CANCELADA mantienen su estado/fechas sin recalcular. La fecha de cierre se trunca a segundos UTC y se ajusta al inicio si el reloj resultara anterior. Una sesión ya completada rechaza cancelación con TransicionSesionInvalidaException y HTTP409 SESION_YA_COMPLETADA.

## Atomicidad y concurrencia

ProcesarRespuestasService.finalizar conserva la etapa original antes de mutar ContextoProcesamiento. Dentro de repo.conRespuestaBloqueada, con lock ordenado sesión→respuesta y fencing vigente:

1. Guarda EvaluacionSTAR.
2. Actualiza la respuesta a EVALUADO/FINALIZADO.
3. Consulta los resultados de la sesión por lotes dentro de la misma transacción.
4. Invoca calcularPromedioGlobal y persiste COMPLETADA/promedio/fecha si procede.
5. El commit del callback confirma todos esos cambios conjuntamente.

No se realizan llamadas externas durante ese bloqueo final. Los saveAndFlush participan en la transacción exterior; no introducen commits independientes.

Se verificaron ambos órdenes de la carrera:

- Cancelar mientras la llamada LLM de P3 permanece bloqueada en el fixture: la API cancela en otra transacción; al liberar el proveedor, el worker solo libera el lease. No crea evaluación P3 ni promedio; conserva dos resultados y motivo de finalización anticipada.
- Completar P3 primero: la cancelación posterior recibe 409 SESION_YA_COMPLETADA y mantiene exactamente promedio y fecha_fin.

También se verificó P3 definitiva por 401 y por agotamiento del ciclo 2 manual. Ambos casos dejan CANCELADA sin promedio y conservan P1/P2. Un caso elimina una transcripción previa y comprueba que tres estados EVALUADO no bastan para completar si falta un bloque requerido.

## API y vistas

GET /api/v1/sesiones/{id} conserva los campos anteriores. PreguntaSesionResponse añade puntaje_general y version_rubrica nullable, puede_reintentar calculado con ContextoProcesamiento y motivo_fallo seguro. SesionDetalleResponse añade motivo_cierre nullable; en CANCELADA diferencia fallo definitivo de finalización anticipada utilizando los datos persistidos, sin cambiar el esquema.

No se exponen public_id_cloud, referencia_audio, procesamiento_contexto ni códigos crudos de proveedor. Las evaluaciones de detalle y panel se leen por lotes, no mediante una consulta por pregunta.

ResumenSesionComponent reutiliza SesionService.consultarSesion:

- COMPLETADA: puesto, área/nivel, fechas UTC, Promedio final y tres puntajes con enlaces MCK09.
- CANCELADA: Incompleta: n de 3 respuestas evaluadas, motivo y resultados disponibles, sin Promedio final.
- INICIADA/EN_PROCESO: progreso incompleto sin promedio y acción Continuar práctica.
- Cada pregunta indica STAR, estado, competencia, puntaje cuando existe y enlace MCK08/MCK09 según corresponda; una pregunta sin respuesta muestra Sin respuesta registrada.
- Nueva práctica abre /puestos directamente en terminales; en una sesión abierta utiliza confirmación explícita de cancelación.

MCK09 elimina el aviso provisional del Incremento 7 y ofrece Ver resumen para P3 o estados terminales, Ver detalle y Volver al panel. P1/P2 EN_PROCESO conservan avance/cancelación/nueva práctica con confirmación. El detalle muestra puntajes individuales y Ver resumen de sesión en terminales. MCK03 muestra la última completada con promedio/contador, Ver resumen y Ver sesión, junto al texto del flujo activo.

## Pruebas ejecutadas

| Comprobación | Resultado final |
| --- | --- |
| Maven H2 | **170 descubiertas, 168 ejecutadas**, 2 exclusivas de MySQL omitidas; 0 fallas/errores |
| Maven MySQL **8.4.13** | **170 ejecutadas**, 0 fallas/errores/omitidas |
| Dominio nuevo | 8 pruebas en SesionEntrevistaTest; media exacta, ceros, faltantes, duplicados, datos inválidos, terminales y fecha |
| Integración nueva | 8 pruebas en EvaluacionIntegrationTest, repetidas en ambos motores; cierre, promedios, D-02 P3, carrera, panel y nueva sesión |
| npm run test:media | 6 aprobadas |
| npm run build | Correcto, sin advertencias de presupuesto |
| mvn package -DskipTests | Correcto; JAR ejecutable |
| Chrome for Testing | **8 escenarios reales**, 0 errores de página y **0 errores de consola** |
| Esquema MySQL | 8 tablas de negocio, 68 columnas, evaluaciones_star=14, V1/V2 intactas y sin V3+, ddl-auto=validate |
| Dependencias | backend/pom.xml, frontend/package.json y package-lock.json sin cambios |
| Git | HEAD ca798f5, índice vacío, Incremento8 sin commit, .tools ignorado |

Los ocho escenarios de Chrome cubren:

1. P1→P2 con STAR→P3 y cierre COMPLETADA con media81.25 y fecha UTC.
2. Resumen completo desktop/móvil390px, tres puntajes (75.00/81.25/87.50), promedio y fechas.
3. Los tres enlaces individuales abren MCK09 y regresan al resumen.
4. Detalle COMPLETADA con puntajes y enlace de resumen.
5. MCK03: promedio81.25, contador1, Ver sesión y Ver resumen funcional.
6. Nueva práctica inmediatamente después del cierre, POST de sesión devuelve201.
7. Cancelar esa práctica tras evaluar P1: resumen1/3, motivo, dos Sin respuesta registrada, sin Promedio final ni puntuaciones de relleno.
8. P1 cancelada permanece consultable; detalle/resumen y Nueva práctica mantienen navegación válida.

Se capturaron cuatro respuestas mediante MediaRecorder de Chrome, cámara/micrófono emulados y upload real. El worker usó ffmpeg, S3/STT/LLM HTTP locales y persistencia MySQL reales. Las respuestas de la aplicación no se interceptaron en Playwright. Los puntajes distintos proceden de respuestas EVAL-1.0 válidas del fixture HTTP, no de modificaciones de la base. Se inspeccionaron capturas del resumen completo desktop y cancelado móvil; ambas variantes también tienen comprobación automática de desbordamiento a390px.

Chrome ya no estaba instalado en la ruta utilizada en el incremento anterior. Se descargó Chrome for Testing estable **156.0.8078.47**, resuelto desde el [manifiesto oficial](https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json), en .tools/chrome-for-testing. No se cambió la instalación del sistema. Una primera corrida detectó 404 de favicon; se añadió un favicon SVG autocontenido y se repitió la compilación y el recorrido completo. La corrida final no filtra esos errores: exige consola y página vacías.

## Reproducción y evidencias

JDK17, Maven3.9.9, ffmpeg/ffprobe9.0.2 y MySQL8.4.13. Las herramientas auxiliares se mantienen bajo .tools, ignoradas por Git. No se agregaron dependencias de aplicación.

Desde backend:

```powershell
mvn test
$env:TEST_DB_URL='jdbc:mysql://127.0.0.1:33307/ep360_incremento8_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:TEST_DB_USERNAME='root'
$env:TEST_DB_PASSWORD=''
mvn test '-Dspring.profiles.active=mysql'
mvn package -DskipTests
```

Usa exclusivamente una base de pruebas. Elimina TEST_DB_URL antes del pase H2. Desde frontend ejecuta npm run test:media y npm run build.

El planificador está deshabilitado en las pruebas de integración, con Clock controlable y llamadas deterministas al worker. Chrome utiliza otra base (ep360_incremento8_browser), JAR18080, Angular14200, S3 local19090 y STT/LLM local19091. Se acelera solo el intervalo a500ms y el backoff a1/1s; producción conserva2000ms y5/15s. Las bases del navegador y Maven no comparten filas.

Scripts versionados:

- scripts/pruebas/pipeline-resumen-http-local.cjs: fixtures S3/STT/LLM; puntajes de criterios configurables sin añadir identidad al payload.
- scripts/pruebas/resumen-incremento8.cjs: flujo completo y cancelado contra la aplicación real; permite seleccionar Chrome mediante EP360_CHROME_BIN. Por defecto usa .tools/chrome-for-testing/chrome-win64/chrome.exe y requiere Playwright auxiliar en .tools/browser-check.
- scripts/pruebas/registrar-evidencias-incremento8.py: valida logs, XML, esquema, Chrome y estado Git antes de consolidar el JSON.

Artefactos:

- [Consolidado](evidencias/pruebas-incremento-8.json): resultados, suites, invariantes, configuración y SHA-256 del JAR.
- [Chrome](evidencias/pruebas-browser-incremento-8.json): ocho casos, DTO de la sesión completa, llamadas API y contadores de errores.
- Logs y capturas .tools/inc8-*: auxiliares ignorados.

## Límites de la verificación

S3, STT y LLM son proveedores HTTP locales de prueba, con clientes y ejecución Java/ffmpeg/MySQL reales. No se certifican cuentas OpenAI, resultados del modelo en producción ni IAM/bucket AWS. Los medios de Chrome son emulados; no acreditan hardware físico.

motivo_cierre utiliza el error definitivo disponible o la finalización anticipada, sin una nueva columna histórica. Historial integral, filtros y comparación permanecen fuera del alcance. RF-17 continúa En progreso.

Flyway11.7.2 conserva su recomendación de actualización al detectar MySQL8.4; las migraciones y validate pasaron sin modificar esquema ni dependencias. Los servicios de prueba se detienen al finalizar la verificación.

El Incremento8 queda **SIN COMMIT ni staging**, listo para auditoría.

