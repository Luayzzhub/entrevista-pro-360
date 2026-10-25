# Verificación del Incremento 5

Fecha de pruebas: 21/10/2026, America/Lima. Cierre 25/10/2026: el Incremento 5 fue auditado y APROBADO sin observaciones por el Orquestador; se confirma en este commit atómico de main y se publica antes de iniciar el Incremento 6. Los resultados siguientes conservan su fecha y alcance de ejecución.

## Cierre Git del Incremento 4

Antes de escribir el Incremento 5 se actualizaron README, matriz, verificación y evidencia JSON del 4 para reflejar la auditoría y aprobación. Se comprobaron las exclusiones de .tools, target, dist, node_modules, capturas y medios temporales.

Commit en main: **dd40f77b33745d48ac09afce1ecbb4d437ae4a73**, mensaje exacto:

```text
feat: implementar permisos de camara y microfono, captura de 60s y revision local MCK05-MCK07
```

Push con código 0:

```text
To https://github.com/Luayzzhub/entrevista-pro-360.git
   214c011..dd40f77  main -> main
```

Antes de construir el 5, git ls-remote confirmó dd40f77b33745d48ac09afce1ecbb4d437ae4a73 refs/heads/main y el árbol quedó limpio. Tras la auditoría sin observaciones, este commit cierra el Incremento 5 con el mensaje exacto: feat: implementar confirmacion idempotente de respuesta, validacion ffprobe, subida S3 y progreso MCK08. Su publicación y sincronización se comprueban con push/status antes de implementar el 6; el hash resultante se registra en el informe de ese incremento.

## Resultados finales

| Verificación | Resultado |
| --- | --- |
| mvn test, H2 2.3.232 | 103 casos descubiertos; **101 ejecutados**, 2 exclusivos de MySQL omitidos; 0 fallos/errores |
| mvn test -Dspring.profiles.active=mysql, MySQL 8.0.44 | **103 ejecutados**, 0 omitidos, 0 fallos/errores |
| Nuevas pruebas de backend | 8 RespuestaVideoTest + 8 FfprobeInspectorMultimediaAdapterTest + 3 S3AlmacenamientoVideoAdapterTest + 11 ConfirmacionRespuestaIntegrationTest = 30 |
| LayerDependenciesTest | Correcto; dominio/aplicación solo Java y capas internas permitidas |
| npm run test:media | **6 correctas**, 0 fallos/omitidas |
| npm run build | AOT/plantillas estrictas correctas; **0 advertencias de presupuesto**; inicial 287.37 kB, transferencia estimada 78.67 kB |
| mvn package -DskipTests | BUILD SUCCESS, JAR ejecutable generado e iniciado contra MySQL |
| Navegador | **7 escenarios correctos**, 0 errores JavaScript; Chrome 155.0.8059.85 / Playwright 1.64.1 |
| Esquema | V1/V2 idénticas al commit base; 8 tablas/68 columnas de negocio; ddl-auto=validate |
| Dependencias | Sin nuevas dependencias Maven/npm; SDK S3 existente pasa a uso real |

Entorno aislado: MySQL 127.0.0.1:33307 con base ep360_incremento5_test; JAR 18080; Angular 14200; endpoint HTTP S3 local 19090. ffprobe/ffmpeg **9.0.2** portables en .tools/ffmpeg, descargados de Gyan (distribuidor enlazado desde el sitio oficial de FFmpeg). El adaptador usa fallback portable o FFPROBE_BIN/PATH; las pruebas usan ffmpeg real para generar sus archivos. Binarios, grabaciones, logs y capturas permanecen ignorados en .tools.

## Dominio, inspección e integración

RespuestaVideo valida sus 10 componentes: ID, clave opaca sin URL/espacios/recorrido, webm/mp4, bytes positivos menores que 50 000 000, duración inclusiva 1..60 antes de redondear, estado/fecha y contexto obligatorios. Los tests rechazan 60.0001/60.0004 y límites inválidos, verifican escala 3 HALF_UP y copia profunda inmutable de PROC-1.0 con 13 campos, nulos, intentos y modelos configurados. La transición INICIADA→EN_PROCESO es pura e idempotente; terminales se rechazan.

ffmpeg generó fixtures reales WebM VP8/Opus, VP9/Opus, MP4 H.264/AAC, WebM streaming, solo video, 0.4 s, 61 s y audio Vorbis no permitido. ffprobe real los acepta/rechaza con códigos tipados; texto renombrado se rechaza como formato. Se comprueban límite de bytes y binario ausente. El fallback de paquetes se verificó con fixture streaming y, además, con la grabación real de Chrome cuya cabecera y streams no incluían duration.

S3Client de AWS SDK v2 2.31.54 envía HTTP firmado a HttpServer local. Se comprueban If-None-Match *, Content-Type/Length, ausencia de ACL pública, bytes reales, 412→HEAD sin sobrescritura, DELETE compensatorio y 503 por fallo/bucket ausente. La metadata SHA-256 impide asociar a un objeto ganador los metadatos de una toma distinta. El adaptador acepta endpoint override con forcePathStyle y usa el proveedor estándar de credenciales en ejecución normal.

ConfirmacionRespuestaIntegrationTest usa seguridad, servicios, JPA, ffprobe y S3Client reales. No tiene una transacción envolvente de test; cada petición usa sus transacciones de aplicación. Cada test crea sus cuentas y elimina únicamente sus filas al terminar. Incluye:

- 202/Location, respuesta SUBIDO/PROC-1.0, sesión EN_PROCESO y pregunta siguiente bloqueada.
- Reenvío con archivo falso y S3 en fallo después de confirmar: mismo ID, sin nueva inspección ni peticiones S3; funciona también después de cancelar.
- 400 por parte ausente/vacía y 413 en el borde de tamaño del controlador. Este caso usa MockMultipartFile con getSize en el borde; no se declara una prueba de envío HTTP de 50 MB.
- 415 por texto/códec inválido; 422 sin audio y por duración corta/larga; 503 de S3: sin fila y sesión INICIADA/pregunta 1 disponible.
- Orden, pregunta ajena al banco, sesión ajena y estados CANCELADA/COMPLETADA; 401/404/400 de consulta; DTO sin claves internas.
- **Dos PUT HTTP concurrentes:** el primero queda demorado en S3; el segundo confirma mientras el primero espera. Ambos devuelven 202 con el mismo ID, una fila y un objeto sin DELETE ni sobrescritura. Esto también prueba que el PUT S3 no retiene el bloqueo de sesión.
- **Cancelación concurrente:** con upload demorado, cancelar concluye antes de liberarlo; la confirmación posterior devuelve 409 y elimina el objeto nuevo sin crear respuesta.
- **Fallo real de persistencia:** un trigger temporal de pruebas rechaza el INSERT. Respuesta/sesión revierten y el objeto nuevo se elimina. El trigger se elimina en finally tanto en H2 como MySQL; no modifica las migraciones aprobadas.

## Recorrido real de Chrome

Se usaron dispositivos de cámara/micrófono emulados por Chromium; MediaStream, MediaRecorder, Blob, multipart y SDK/ffprobe fueron reales. No se sustituyeron respuestas del backend. El fallo de red se emuló con modo offline y el 503 provino del endpoint S3 local configurado en fallo.

1. Cuenta/sesión reales, cámara/micrófono activos, grabación de más de 1 s y revisión: confirmar habilitado.
2. Offline: mensaje de red, URL local intacta y botón disponible para reintentar.
3. S3 devuelve 503: progreso y aviso de pestaña abierta visibles, confirmar/repetir bloqueados durante la petición; al fallar ambos vuelven a estar disponibles, sin revocar la toma ni crear una respuesta. Sesión sigue INICIADA.
4. Se restablece S3 y se confirma **la misma toma**: 202 con Location /api/v1/respuestas/9, navegación a /sesiones/26/respuestas/9 y URL local revocada. MySQL contiene una sola fila SUBIDO, etapa EXTRAER_AUDIO y último hito SUBIDO.
5. El archivo almacenado tiene **125833 bytes** y ffprobe reporta **2.419 s**. Se comprobó que format.duration y stream.duration faltaban: el servidor obtuvo la duración desde los paquetes. Acceso anónimo al objeto local: 403; SDK usa escrituras condicionales firmadas.
6. MCK 08 muestra metadatos de servidor y hitos, sondea con intervalo observado de **3040 ms**, actualiza manualmente y no expone public_id_cloud, referencia_audio, bloqueo_hasta_utc, version_ejecucion ni procesamiento_contexto. Capturas de escritorio/390 px inspeccionadas, sin desborde.
7. Panel EN_PROCESO con 0/3 evaluadas; al salir se detiene el sondeo. Reabrir la sesión muestra Video subido en pregunta 1, ninguna pregunta habilitada y dos bloqueadas; su enlace vuelve a MCK 08.

Los IDs y números corresponden a esta ejecución, no a una semilla. Evidencia versionable: [pruebas-incremento-5.json](evidencias/pruebas-incremento-5.json). Capturas locales auxiliares: .tools/inc5-reintento.png, inc5-procesamiento.png e inc5-procesamiento-movil.png.

## Límites

RF-09/RF-11/RF-12 quedan Verificados en el alcance de este incremento. La integración S3 se acredita con el SDK real y HTTP local; no se aprovisionaron recursos AWS ni se certificaron políticas/IAM de una cuenta de producción. El endpoint local comprueba presencia de firma y cabeceras/semántica condicional; no es un emulador completo de autenticación/políticas AWS.

ffprobe y PUT S3 corren sin transacción MySQL; el registro final usa lock y un HEAD breve. S3 y MySQL no comparten transacción XA: la compensación es best effort y evita borrar un objeto ya referenciado por una confirmación concurrente. Si no se puede comprobar un commit incierto por indisponibilidad de BD, se preserva el objeto para reconciliación con la misma toma/clave estable. No se promete atomicidad distribuida frente a cualquier caída.

No hay trabajador Scheduled, extracción de audio de respuestas, STT, métricas ni evaluación. La respuesta permanece SUBIDO, con 0 intentos en PROC-1.0 y sin promedio de sesión. Extracción/transcripción/métricas corresponden al Incremento 6; EVAL/COM-1.0 y MCK 09 al 7. La UI informa esta disponibilidad y conserva el mensaje de flujo de segundo plano acordado.
