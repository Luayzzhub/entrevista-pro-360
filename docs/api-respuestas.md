# API de respuestas — Incrementos 5 a 7

Todas las rutas requieren Bearer JWT válido y cuenta ACTIVO/POSTULANTE. El propietario procede exclusivamente del principal autenticado. Fechas UTC ISO 8601 con Z y precisión de segundos; JSON snake_case. El archivo se inspecciona por su contenido: nombre, extensión y MIME suministrados por el cliente no deciden su aceptación.

## PUT /api/v1/sesiones/{id}/preguntas/{idPregunta}/respuesta

Consume multipart/form-data, parte obligatoria **video**, sin otros datos de identidad ni metadatos declarados por el cliente. id es Long positivo e idPregunta es Integer positivo. El controlador copia el stream a un archivo temporal de nombre aleatorio y lo elimina en finally.

Reglas del servidor:

- Tamaño entero estrictamente positivo y menor que 50 000 000 bytes. Los límites de multipart de Spring son 50MB por archivo y 52MB por petición; además se aplica el borde decimal estricto de 50 000 000.
- WebM con VP8/VP9 + Opus o MP4 con H.264 + AAC, con pistas reales de video y audio.
- Duración real entre 1.000 y 60.000 s inclusivos, validada antes de redondear; se guarda a escala 3 HALF_UP. Un 60.0001 no se acepta como 60.000.
- ffprobe obtiene format.duration, luego el máximo stream.duration y, si ambos faltan, el máximo pts_time + duration_time de los paquetes. Este último caso permite aceptar WebM streaming de MediaRecorder sin duración en cabecera.
- La sesión propia debe seguir abierta y la pregunta debe ser la habilitada. Las tres preguntas del puesto se validan.

Éxito inicial y reenvío idempotente: **202 Accepted**, Location: /api/v1/respuestas/{id_respuesta}. ConfirmacionRespuestaResponse contiene exactamente:

```json
{
  "id_respuesta": 9,
  "id_sesion": 26,
  "id_pregunta": 1,
  "orden_pregunta": 1,
  "estado_sesion": "EN_PROCESO",
  "estado_procesamiento": "SUBIDO",
  "etapa_actual": "EXTRAER_AUDIO",
  "ultimo_hito_valido": "SUBIDO",
  "duracion_segundos": 2.419,
  "formato": "webm",
  "tamano_bytes": 125833,
  "fecha_grabacion": "2026-10-21T07:50:39Z",
  "ruta_consulta": "/api/v1/respuestas/9"
}
```

Ejemplo observado en la verificación, no semilla. En un reenvío se devuelve la respuesta existente y el estado actual de su sesión, incluso si esta ya es terminal o avanzó. No se reemplaza la respuesta; no se vuelve a ejecutar ffprobe/S3. La parte video sigue siendo obligatoria/no vacía y está sujeta a los límites de transporte del controlador.

| HTTP | Código | Causa |
| --- | --- | --- |
| 400 | DATOS_INVALIDOS | Falta video o está vacío; errores_campo.video |
| 400 | PARAMETRO_INVALIDO | ID no entero positivo, fuera del rango de su tipo |
| 401 | NO_AUTENTICADO | JWT ausente/inválido o cuenta no habilitada |
| 404 | SESION_NO_ENCONTRADA | Sesión inexistente o ajena; no se revela la diferencia |
| 404 | PREGUNTA_NO_ENCONTRADA | Pregunta ajena al banco de tres de esta sesión |
| 409 | CATALOGO_INVALIDO | El banco no cumple las tres preguntas activas/órdenes |
| 409 | PREGUNTA_NO_HABILITADA | No hay pregunta habilitada o no es la solicitada |
| 409 | SESION_NO_ADMITE_RESPUESTAS | Sesión terminal sin respuesta previa para esa pregunta |
| 413 | ARCHIVO_DEMASIADO_GRANDE | bytes >= 50 000 000 o límite multipart excedido |
| 415 | FORMATO_NO_ADMITIDO | Contenedor/códecs no admitidos o archivo corrupto/falso |
| 422 | ARCHIVO_NO_UTILIZABLE | Sin alguna pista, duración inválida/no obtenible o tamaño inconsistente |
| 503 | MULTIMEDIA_NO_DISPONIBLE | ffprobe ausente, ejecución/lectura no disponible o timeout |
| 503 | ALMACENAMIENTO_NO_DISPONIBLE | Bucket sin configurar, error S3, reconciliación fallida o contenido distinto al objeto existente |
| 500 | ERROR_INTERNO | Fallo de persistencia inesperado; sin trazas/proveedor en la respuesta |

La autorización/idempotencia inicial ocurre bajo bloqueo de sesión y termina antes de inspeccionar/subir. Después, ffprobe y PUT S3 se ejecutan fuera de la transacción. La clave estable es usuarios/{usuario}/sesiones/{sesion}/preguntas/{pregunta}/respuesta.{formato}. S3 recibe If-None-Match: *, MIME canónico y longitud real; no se solicita ACL pública. Un 412/ConditionalRequestConflict se reconcilia con HEAD, comparando tamaño y la huella SHA-256 guardada si está presente, sin sobrescribir una toma distinta.

La transacción final vuelve a bloquear la sesión y comprobar existencia, estado y orden; una respuesta concurrente ganadora se devuelve con su mismo ID. Un HEAD breve comprueba la presencia del objeto antes de insertar. Se guardan respuesta SUBIDO y sesión EN_PROCESO en la misma transacción. Al compensar un objeto nuevo se comprueba bajo el mismo bloqueo que no haya quedado referenciado por una confirmación concurrente. DELETE es best effort; un commit incierto/BD inaccesible no permite borrar a ciegas un posible objeto confirmado. Un reintento de la misma toma reconcilia la clave estable. No existe transacción distribuida XA entre S3 y MySQL.

El contexto inicial no se expone entero por API. Tiene exactamente:

```json
{
  "version_contexto": "PROC-1.0",
  "etapa_actual": "EXTRAER_AUDIO",
  "ultimo_hito_valido": "SUBIDO",
  "ciclo_actual": 1,
  "intentos_por_etapa": {
    "EXTRAER_AUDIO": {"ciclo_1": 0, "ciclo_2": 0},
    "TRANSCRIBIR": {"ciclo_1": 0, "ciclo_2": 0},
    "METRICAS": {"ciclo_1": 0, "ciclo_2": 0},
    "EVALUAR": {"ciclo_1": 0, "ciclo_2": 0}
  },
  "reintento_manual_usado": false,
  "ultimo_error": null,
  "proximo_intento_utc": null,
  "bloqueo_hasta_utc": null,
  "version_ejecucion": 0,
  "referencia_audio": null,
  "modelo_stt": "<STT_MODEL configurado>",
  "modelo_llm": "<LLM_MODEL configurado>"
}
```

## GET /api/v1/respuestas/{id}

ID Long positivo. **200 OK** para el propietario con RespuestaEstadoResponse y exactamente estos campos:

| Campos | Contenido |
| --- | --- |
| id_respuesta, id_sesion, id_pregunta, orden_pregunta | Identidad y orden propios |
| enunciado_pregunta, competencia_evaluada, aplica_star, nombre_puesto | Contexto del banco persistido |
| estado_sesion, estado_procesamiento | Estados reales |
| etapa_actual, ultimo_hito_valido | Dos hitos públicos del contexto |
| duracion_segundos, formato, tamano_bytes, fecha_grabacion | Metadatos confirmados por el servidor |
| puede_reintentar | true solo con sesión EN_PROCESO, respuesta FALLIDO, error recuperable, ciclo 1, manual no usado y lease libre |
| error | null normalmente; mensaje español que distingue reintento disponible y falla definitiva/CANCELADA |
| metricas | null hasta METRICAS; luego conteo_palabras, palabras_por_minuto, conteo_muletillas y version_metricas |

Sesión ajena y respuesta inexistente devuelven el mismo **404 RESPUESTA_NO_ENCONTRADA**, mensaje: La respuesta solicitada no existe. ID inválido: 400 PARAMETRO_INVALIDO. La respuesta nunca incluye public_id_cloud, referencia_audio, bloqueo_hasta_utc, version_ejecucion ni procesamiento_contexto.

MCK 08 consulta cada 3 s mientras el estado sea SUBIDO/AUDIO_EXTRAIDO/TRANSCRITO y la sesión siga abierta, incluyendo EVALUAR. Se detiene ante EVALUADO/FINALIZADO, FALLIDO, sesión terminal o al salir. Un error de lectura permite Actualizar estado. En EVALUADO ofrece Ver resultado de tu respuesta y habilita la siguiente pregunta.

## POST /api/v1/respuestas/{id}/reintento

Sin campos de entrada. JWT y propiedad obligatorios; una respuesta ajena o inexistente devuelve 404 RESPUESTA_NO_ENCONTRADA. Bajo locks de sesión y respuesta requiere EN_PROCESO, FALLIDO, error recuperable, ciclo 1, reintento_manual_usado=false y lease nulo o estrictamente anterior a ahora. Si falta cualquier condición: 409 REINTENTO_NO_DISPONIBLE.

Devuelve 202 Accepted con RespuestaEstadoResponse actualizado. Persiste ciclo_actual=2, reintento_manual_usado=true, error/lease nulos y próximo intento ahora UTC. Restaura exactamente el último hito y conserva la etapa fallida. Un segundo reintento manual produce 409. Angular muestra Reintentar análisis y reanuda el seguimiento tras 202.

## Worker durable PROC-1.0

Cada 2 s, configurable y secuencial por JVM, selecciona sesiones abiertas y respuestas SUBIDO/AUDIO_EXTRAIDO/TRANSCRITO con etapa EXTRAER_AUDIO/TRANSCRIBIR/METRICAS/EVALUAR. Excluye FINALIZADO y EVALUADO. Solo admite próximo intento <= ahora y lease nulo o < ahora. El claim corto incrementa versión y contador del ciclo antes de operar y fija lease a 90 s. Las escrituras finales y heartbeat (cada 20 s, hilo separado) comprueban versión/etapa/lease bajo lock. Orden de locks: sesión y luego respuesta; ffmpeg y HTTP se ejecutan fuera de transacciones.

EXTRAER_AUDIO descarga video privado a temporal, ejecuta ffmpeg sin shell (60 s) y verifica WAV PCM s16le mono 16000 Hz con muestras. Lo sube como audio/wav a usuarios/{usuario}/sesiones/{sesion}/preguntas/{pregunta}/audio.wav. Un hito de audio/transcripción y HEAD válido evita repetir ffmpeg. TRANSCRIBIR reutiliza una fila existente o envía únicamente file=audio.wav, model del contexto, language=es y response_format=json al endpoint /v1/audio/transcriptions (60 s). No añade identidad del usuario. Guarda texto trim, confianza_stt=NULL, idioma=es y fecha UTC.

METRICAS se calcula en dominio puro: NFKC; tokens Unicode L/Nd con guion o apóstrofo interno; PPM con duración servidor y BigDecimal HALF_UP, escala 2; siete muletillas exactas y prioridad de o sea solo entre espacios. Omite ceros y suma el detalle. Upsert por id_respuesta. Al concluir: TRANSCRITO/EVALUAR, hito TRANSCRITO, error/próximo/lease nulos. No se invoca LLM ni se evalúa/completa sesión.

Cada etapa admite tres ejecuciones por ciclo. Timeout/I/O/408/429/5xx programa max(5 s, Retry-After) tras intento 1 y max(15 s, Retry-After) tras intento 2. Tercera falla del ciclo 1 deja FALLIDO recuperable y sesión EN_PROCESO. Ciclo 2 agotado, credenciales/configuración ausentes, rechazo no transitorio, audio inutilizable, transcripción vacía o sin palabras son definitivos: FALLIDO y CANCELADA en la misma transacción, fecha_fin >= fecha_inicio, promedio null. Un tercer claim caído se agota al expirar sin ejecutar un cuarto intento. Una cancelación durante la operación solo libera lease al finalizar; no persiste su resultado.

## EVALUAR — EVAL-1.0 / COM-1.0 (Incremento 7)

Tras METRICAS, un nuevo claim ejecuta EVALUAR fuera de transacción. Exige transcripción no vacía y MET-1.0 persistidas. Una evaluación existente válida evita otra llamada al proveedor. El puerto recibe exclusivamente version_contrato, version_rubrica, puesto, pregunta, competencia_evaluada, aplica_star y transcripcion; no añade IDs, nombre ni correo del postulante.

OpenAiEvaluacionAdapter llama POST /v1/responses con el modelo del contexto (por defecto gpt-5.4-mini-2026-03-17), store=false, sin tools y text.format={type:json_schema,name:evaluacion_eval_1_0,strict:true,schema:...}. Esquema versionado en backend/src/main/resources/llm/eval-1.0.schema.json. Todos sus objetos son cerrados y todos los campos son obligatorios; detalle_star admite null, condicionado finalmente en dominio al valor real de la pregunta. Timeout total de 60 s, cuerpo máximo 2 MB, sin reintentos ocultos.

ValidadorEvaluacion exige las versiones exactas, cuatro criterios y puntajes enteros discretos 0/25/50/75/100 (rechaza también 25.0), justificaciones trim de 1..600 y textos formativos trim de 1..1200. STAR es null si no aplica; si aplica requiere situacion/tarea/accion/resultado con estado PRESENTE/PARCIAL/AUSENTE y observación trim de 1..600. Campos extra o faltantes invalidan el contrato.

El backend calcula (relevancia+claridad+estructura+evidencias)/4 con BigDecimal HALF_UP y escala 2. Guarda los cinco puntajes a escala 2. areas_mejora conserva exactamente version_contrato=EVAL-1.0, justificaciones de los cuatro criterios y sintesis. No almacena un puntaje general suministrado por el modelo.

Refusal, status distinto de completed, JSON mal formado, claves duplicadas y violaciones del contrato producen SALIDA_EVALUACION_INVALIDA recuperable, sin insertar ni actualizar evaluación. HTTP408/429/5xx y timeout/red también son recuperables. Configuración/credenciales ausentes y rechazos no transitorios son definitivos. Se conserva TRANSCRITO/EVALUAR durante intentos 1 y 2; al agotar ciclo 1 queda FALLIDO recuperable y el único manual restaura TRANSCRITO/EVALUAR, sin repetir audio/STT/MET. Agotar ciclo 2 cancela conforme a D-02.

Bajo lock sesión→respuesta y fencing se guardan evaluación y EVALUADO/FINALIZADO en la misma transacción. Hito EVALUADO, error/próximo/lease nulos. Cancelar durante la llamada libera lease sin guardar evaluación. No se calcula promedio global ni se cierra sesión. Las tres respuestas evaluadas conservan EN_PROCESO, puntaje_promedio=null y fecha_fin=null hasta Incremento 8.

## Bloques de resultado en GET /api/v1/respuestas/{id}

Se conservan los campos previos. Cada bloque es null hasta existir:

- transcripcion: texto_transcrito, idioma, fecha_transcripcion (UTC).
- metricas: conteo_palabras, palabras_por_minuto, conteo_muletillas, detalle_muletillas, version_metricas y fecha_calculo (UTC).
- evaluacion: puntaje_general, version_rubrica, version_contrato, criterios (cuatro objetos puntaje/justificacion), aplica_star, detalle_star nullable, fortaleza_principal, sintesis_areas_mejora, recomendaciones y fecha_evaluacion (UTC).

Las justificaciones y síntesis se reconstruyen del JSON areas_mejora. No se exponen contexto, modelos internos, lease ni public_id_cloud. Un resultado de una sesión cancelada permanece consultable por su propietario.

## POST /api/v1/respuestas/{id}/reproduccion — PA-04

Sin campos de entrada. JWT válido y propiedad obligatorios, también si la sesión ya fue cancelada. No impone EVALUADO como condición de autorización. Comprueba HEAD antes de firmar GET:

| HTTP | Código/cuerpo | Comportamiento |
| --- | --- | --- |
| 200 | id_respuesta, video_url_cloud, expira_en | Cache-Control: no-store; SigV4 GET HTTP/HTTPS, 300 s desde Clock |
| 401 | NO_AUTENTICADO | JWT ausente/inválido o cuenta no habilitada |
| 404 | RESPUESTA_NO_ENCONTRADA | Ajena/inexistente indistinguibles; no se consulta S3 para otra cuenta |
| 404 | VIDEO_NO_DISPONIBLE | Mensaje exacto Video no disponible.; conserva evaluación, transcripción y métricas |
| 503 | ALMACENAMIENTO_NO_DISPONIBLE | HEAD/firma/credenciales/bucket no disponibles |
| 400 | PARAMETRO_INVALIDO | ID no positivo o fuera de rango |

Ejemplo de forma (el enlace real incluye X-Amz-Algorithm, Credential, Date, Expires=300, SignedHeaders y Signature):

```json
{
  "id_respuesta": 9,
  "video_url_cloud": "https://bucket.s3.us-east-1.amazonaws.com/...?...",
  "expira_en": "2026-10-25T18:05:00Z"
}
```

La URL no se persiste. Su emisión no altera estado, evaluación ni fechas de sesión. El adaptador usa firma S3 equivalente del SDK existente con signingClockOverride; el endpoint local respeta la misma clave/ruta. Los fixtures verifican HMAC, expiración y manipulación de query.

## Navegación MCK08/MCK09

Ruta protegida: /sesiones/:idSesion/respuestas/:idRespuesta/resultado. Valida ambos IDs y la asociación sesión/respuesta. MCK08 y el detalle de sesión ofrecen Ver resultado de tu respuesta al alcanzar EVALUADO.

MCK09 presenta puesto/pregunta/competencia, puntaje /100 y cuatro justificaciones de peso 25 %, fortaleza, mejora y recomendaciones como texto plano. STAR es cualitativo o muestra STAR no aplica a esta pregunta. MET-1.0 y transcripción aparecen en secciones separadas del puntaje y del video. Al abrir, solicita reproducción; Renovar enlace de video repite POST. Hay renovación al vencer el enlace y ante error del reproductor posterior a expira_en. VIDEO_NO_DISPONIBLE retira el reproductor y conserva los bloques del resultado.

P1/P2 con sesión EN_PROCESO: Siguiente pregunta abre el detalle actualizado; Finalizar anticipadamente pide confirmación; Nueva práctica requiere confirmar la cancelación antes de /puestos. Nunca se ofrece volver a grabar o reemplazar video. P3 ofrece volver a sesión/panel y explica que cierre y resumen global corresponden al Incremento 8.
