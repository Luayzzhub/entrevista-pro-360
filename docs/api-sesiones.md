# API de sesiones — Incremento 3

Prefijo `/api/v1/sesiones`. Todas las operaciones requieren Bearer JWT válido y cuenta ACTIVO/POSTULANTE. El propietario se obtiene exclusivamente de `Authentication.getPrincipal()` (`Long`); no se acepta `id_usuario` en los DTO de escritura. JSON `snake_case`, fechas UTC ISO 8601 con `Z` y precisión de segundos.

## POST /api/v1/sesiones

Entrada cerrada:

```json
{"id_puesto":1}
```

`id_puesto` es un entero positivo obligatorio. Nulo/ausente/no positivo: 400 `DATOS_INVALIDOS`, con `errores_campo.id_puesto`. Tipos inválidos o decimales: 400; propiedades desconocidas, incluido `id_usuario`: 400 `CAMPOS_NO_PERMITIDOS`.

La transacción bloquea primero la fila del usuario con `PESSIMISTIC_WRITE`. Comprueba que no tenga sesión INICIADA/EN_PROCESO, valida puesto y tres preguntas activas con órdenes 1/2/3 y persiste mediante `saveAndFlush`, manteniendo el bloqueo hasta commit. Usuario con práctica pendiente: 409 con el siguiente contrato:

```json
{
  "codigo":"SESION_ACTIVA_EXISTENTE",
  "mensaje":"Ya tienes una sesión de práctica en curso. Continúala o finalízala antes de iniciar otra.",
  "errores_campo":{"id_sesion_activa":"10"}
}
```

Puesto inexistente/inactivo: 404 `PUESTO_NO_ENCONTRADO`. Catálogo inválido: 409 `CATALOGO_INVALIDO`. Éxito: **201 Created**, `Location: /api/v1/sesiones/{id_sesion}` y `SesionDetalleResponse`. La sesión nace INICIADA, promedio/fin nulos, sin respuestas, con pregunta 1 habilitada. No se genera ninguna respuesta ni evaluación al crearla.

## GET /api/v1/sesiones/{id}

ID entero positivo tipo Long; inválido/no positivo/desbordado: 400 `PARAMETRO_INVALIDO`. Si no existe o no pertenece al usuario, la respuesta es idéntica:

```json
{"codigo":"SESION_NO_ENCONTRADA","mensaje":"La sesión solicitada no existe.","errores_campo":{}}
```

Éxito: **200 OK** con `SesionDetalleResponse`.

| Campo | Tipo / regla |
| --- | --- |
| `id_sesion`, `id_usuario` | Enteros Long de la sesión propia |
| `id_puesto` | Entero del puesto |
| `nombre_puesto`, `area_puesto`, `nivel_experiencia` | Strings leídos del puesto persistido |
| `estado_sesion` | INICIADA, EN_PROCESO, COMPLETADA o CANCELADA |
| `puntaje_promedio` | Número decimal con escala 2 en COMPLETADA; null en los demás estados |
| `fecha_inicio` | UTC ISO 8601 |
| `fecha_fin` | UTC ISO 8601 en estados terminales; null en curso |
| `total_preguntas` | 3 |
| `respuestas_confirmadas` | Cantidad real de respuestas persistidas de la sesión |
| `respuestas_evaluadas` | Cantidad real cuyo estado es EVALUADO |
| `pregunta_habilitada` | Pregunta completa del catálogo o null |
| `preguntas` | Tres objetos `PreguntaSesionResponse`, ordenados 1, 2 y 3 |

`pregunta_habilitada`, cuando existe, contiene exactamente `id_pregunta`, `id_puesto`, `enunciado`, `competencia_evaluada`, `orden`, `tiempo_limite_seg`, `activo` y `aplica_star`.

Cada `PreguntaSesionResponse` contiene `id_pregunta`, `orden`, `enunciado`, `competencia_evaluada`, `tiempo_limite_seg`, `aplica_star`, `habilitada` (boolean), `id_respuesta` (Long/null) y `estado_procesamiento` (SUBIDO/AUDIO_EXTRAIDO/TRANSCRITO/EVALUADO/FALLIDO/null). No se exponen claves S3, contexto JSON de procesamiento ni entidades JPA.

## Regla de avance

Se recorren las tres preguntas en orden. Se habilita la primera sin respuesta solo si todas las anteriores están EVALUADO. Una respuesta SUBIDO/AUDIO_EXTRAIDO/TRANSCRITO/FALLIDO bloquea el avance. Tres respuestas EVALUADO no habilitan una cuarta pregunta. Los estados terminales no habilitan ninguna.

Este incremento lee respuestas reales existentes; todavía no incorpora confirmación de video, procesamiento ni cierre automático de una sesión al completar las evaluaciones. Esas operaciones se integrarán con los incrementos de multimedia/análisis.

## POST /api/v1/sesiones/{id}/cancelacion

Sin cuerpo, o JSON vacío `{}`. Propiedades desconocidas: 400 `CAMPOS_NO_PERMITIDOS`. ID y autorización por propietario iguales a la consulta. Se bloquea la fila de la sesión con `PESSIMISTIC_WRITE` durante la transición y su persistencia.

- INICIADA/EN_PROCESO → CANCELADA, promedio null y fecha final UTC actual truncada a segundos, nunca anterior al inicio.
- CANCELADA → 200 idempotente, conserva la fecha final original y no altera respuestas.
- COMPLETADA → 409 `SESION_YA_COMPLETADA`, mensaje `La sesión ya fue completada y no puede cancelarse.`.

Éxito: **200 OK** con `SesionDetalleResponse` actualizado. Tras cancelar puede crearse otra práctica. No se borran los resultados existentes.

## GET /api/v1/sesiones

**200 OK** con `ResumenSesionesResponse` del propietario:

```json
{"sesion_pendiente":null,"ultima_sesion_completada":null,"total_sesiones_completadas":0,"items":[]}
```

El ejemplo es el contrato para un usuario sin sesiones. En usuarios con datos:

- `sesion_pendiente`: INICIADA/EN_PROCESO actual o null.
- `ultima_sesion_completada`: COMPLETADA con mayor `fecha_fin`, desempate `id_sesion DESC`, o null.
- `total_sesiones_completadas`: cantidad real de COMPLETADA del usuario.
- `items`: todas sus sesiones, `fecha_inicio DESC, id_sesion DESC`, sin paginación en este incremento.

Cada `SesionResumenResponse` contiene exactamente `id_sesion`, `id_puesto`, `nombre_puesto`, `area_puesto`, `nivel_experiencia`, `estado_sesion`, `puntaje_promedio`, `fecha_inicio`, `fecha_fin`, `total_preguntas`, `respuestas_confirmadas`, `respuestas_evaluadas` y `orden_pregunta_habilitada` (1..3/null).

Los puestos se resuelven también para sesiones existentes aunque el puesto esté inactivo. No se consulta ni revela información de sesiones de otros usuarios. La pantalla de historial completo y sus filtros pertenecen a un incremento posterior.
