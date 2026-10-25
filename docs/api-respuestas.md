# API de respuestas — Incremento 5

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
| puede_reintentar | false en Incremento 5 |
| error | null normalmente; mensaje genérico comprensible si FALLIDO, sin datos internos del proveedor |

Sesión ajena y respuesta inexistente devuelven el mismo **404 RESPUESTA_NO_ENCONTRADA**, mensaje: La respuesta solicitada no existe. ID inválido: 400 PARAMETRO_INVALIDO. La respuesta nunca incluye public_id_cloud, referencia_audio, bloqueo_hasta_utc, version_ejecucion ni procesamiento_contexto.

MCK 08 consulta cada 3 s después de cada respuesta de lectura mientras el estado sea SUBIDO/AUDIO_EXTRAIDO/TRANSCRITO. Al salir o terminar se detiene; ante error de lectura permite Actualizar estado. El trabajador automático se implementará en Incremento 6 y la evaluación en el 7: este incremento deja la respuesta en SUBIDO y la pregunta siguiente bloqueada hasta EVALUADO.
