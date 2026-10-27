# API de sesiones — Incrementos 3 a 9

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
| `motivo_cierre` | null salvo CANCELADA; mensaje seguro que distingue fallo definitivo de finalización anticipada |

`pregunta_habilitada`, cuando existe, contiene exactamente `id_pregunta`, `id_puesto`, `enunciado`, `competencia_evaluada`, `orden`, `tiempo_limite_seg`, `activo` y `aplica_star`.

Cada PreguntaSesionResponse conserva id_pregunta, orden, enunciado, competencia_evaluada, tiempo_limite_seg, aplica_star, habilitada, id_respuesta y estado_procesamiento. Añade puntaje_general y version_rubrica (ambos null sin evaluación), puede_reintentar (elegibilidad real de ContextoProcesamiento) y motivo_fallo (mensaje español seguro solo en FALLIDO). No se exponen claves S3, contexto JSON, códigos crudos de proveedor ni entidades JPA.

## Regla de avance

Se recorren las tres preguntas en orden. Se habilita la primera sin respuesta solo si todas las anteriores están EVALUADO. Una respuesta SUBIDO/AUDIO_EXTRAIDO/TRANSCRITO/FALLIDO bloquea el avance. Tres respuestas EVALUADO no habilitan una cuarta pregunta. Los estados terminales no habilitan ninguna.

El worker confirma EVALUADO/FINALIZADO y comprueba el cierre en la misma transacción bajo lock sesión→respuesta. Solo EN_PROCESO puede completar y requiere exactamente tres respuestas de las tres preguntas activas distintas (órdenes 1/2/3), todas EVALUADO con transcripción no vacía, MET-1.0 y evaluación válida según ValidadorEvaluacion.validarGuardada. Puntaje promedio = (puntaje_general1+2+3)/3, BigDecimal HALF_UP, escala 2. Ejemplos: 75.00/81.25/87.50→81.25; 0.00/0.00/6.25→2.08; tres ceros válidos→0.00. Con datos incompletos/inválidos no se cierra ni se calcula promedio. No se reemplazan datos faltantes con ceros.

COMPLETADA recibe fecha_fin UTC a segundos, ajustada al inicio si el reloj resulta anterior. Es idempotente y conserva promedio/fin. CANCELADA nunca puede completarse. Si el usuario cancela mientras EVALUAR P3 sigue en el proveedor, se libera lease sin publicar evaluación P3 ni promedio. Si el worker completa primero, la cancelación posterior devuelve 409 SESION_YA_COMPLETADA. La falla definitiva en cualquier etapa de P3 cancela conservando P1/P2.

## POST /api/v1/sesiones/{id}/cancelacion

Sin cuerpo, o JSON vacío `{}`. Propiedades desconocidas: 400 `CAMPOS_NO_PERMITIDOS`. ID y autorización por propietario iguales a la consulta. Se bloquea la fila de la sesión con `PESSIMISTIC_WRITE` durante la transición y su persistencia.

- INICIADA/EN_PROCESO → CANCELADA, promedio null y fecha final UTC actual truncada a segundos, nunca anterior al inicio.
- CANCELADA → 200 idempotente, conserva la fecha final original y no altera respuestas.
- COMPLETADA → 409 `SESION_YA_COMPLETADA`, mensaje `La sesión ya fue completada y no puede cancelarse.`.

Éxito: **200 OK** con `SesionDetalleResponse` actualizado. Tras cancelar puede crearse otra práctica. No se borran los resultados existentes.

motivo_cierre se deriva de las respuestas persistidas, sin añadir columnas: cuando hay un FALLIDO con ultimo_error.recuperable=false, informa una falla definitiva; en la finalización anticipada informa que la sesión fue finalizada por el postulante. Una sesión cancelada tras un fallo recuperable continúa siendo una finalización anticipada. El JSON de error interno nunca se serializa.

## GET /api/v1/sesiones — Historial D-10 / PM-17 (Incremento 9)

**200 OK**, exclusivamente sesiones del propietario JWT. Conserva los campos del panel MCK03: `sesion_pendiente`, `ultima_sesion_completada` y `total_sesiones_completadas` se calculan sobre **todas** sus sesiones, independientemente de los filtros y la página.

| Parámetro opcional | Regla |
| --- | --- |
| id_puesto | Integer >=1; ausente: Todos |
| fecha_desde / fecha_hasta | LocalDate ISO YYYY-MM-DD; ausentes: sin límite |
| estado | INICIADA, EN_PROCESO, COMPLETADA o CANCELADA; ausente: Todos |
| version_rubrica | Solo COM-1.0; ausente: Todas |
| page | Entero >=0, predeterminado0 |
| size | Entero1..50, predeterminado10 |

Tipos, fechas y enums inválidos:400 PARAMETRO_INVALIDO; rangos y rúbrica desconocida:400 DATOS_INVALIDOS. Fecha desde posterior a hasta: mensaje exacto **La fecha inicial no puede ser posterior a la fecha final.**, con errores_campo.fecha_desde y fecha_hasta. No se aceptan fechas finales fuera del rango representable.

Las fechas filtran **fecha_inicio**: desde a las00:00 en America/Lima convertido a UTC (inclusivo), hasta al inicio del día siguiente en Lima convertido a UTC (exclusivo). Por ejemplo hasta2026-10-16 incluye2026-10-17T04:30:00Z y excluye2026-10-17T05:00:00Z.

D-10: COM-1.0 requiere COMPLETADA, promedio/fin persistidos y exactamente tres respuestas distintas EVALUADO de esa sesión y sus tres evaluaciones COM-1.0 asociadas. No deriva rúbrica del catálogo actual ni de una columna de sesión. Estados incompatibles intersectan con cero filas. Se publica aviso_filtro_rubrica = **El filtro de rúbrica incluye únicamente sesiones completas**; sin filtro: null.

La lista se ordena fecha_inicio DESC, id_sesion DESC, y después se pagina:

- pagina: page solicitado; tamano_pagina: size solicitado.
- total_items_filtrados: filas antes de paginar; total_paginas: techo(total/size), o0.
- items: sublista de esa página; página fuera del conjunto devuelve[] (también para Integer.MAX_VALUE, sin desbordar offset).
- Cada item conserva los13 campos anteriores y añade version_rubrica (COM-1.0 si cumple D-10; null en otro caso) y motivo_cierre (mensaje seguro de cancelación, o null).
- puestos_historial: lista adicional de id_puesto/nombre_puesto de las sesiones propias, incluidos puestos inactivos, antes de filtrar/paginar. Angular la une al catálogo para mantener las opciones históricas.

### comparacion

Objeto siempre presente con habilitada, id_puesto, nombre_puesto, version_rubrica, total_sesiones_comparadas, promedio_conjunto, mejor_puntaje, cambio_vs_primera, cambio_vs_anterior y serie.

Habilitada solo con puesto concreto y COM-1.0. Deshabilitada: total0, serie[] y los cuatro indicadores decimales null. Habilitada con cero completas: mismo vacío, sin inventar0.00. Con una: media y mejor reales; cambios null.

Se calcula sobre **todo el conjunto filtrado antes de paginar**. Orden fecha_fin ASC, id_sesion ASC. Media aritmética de promedios persistidos, BigDecimal divide(n,2,HALF_UP); mejor máximo, cambios último−primero y último−penúltimo a escala2 HALF_UP. Caso PM-17 [87.50,75.00,79.17,83.33]: total4, media81.25, mejor87.50, cambios−4.17 y+4.16.

Cada punto contiene id_sesion, fecha_inicio y fecha_fin OffsetDateTime UTC ISO8601, puntaje_promedio escala2, version_rubrica COM-1.0. No publica claves S3, contexto de procesamiento ni datos de otro usuario.

Respuestas y evaluaciones se leen en lotes findBySesionIdInOrderByIdAsc / findByRespuestaIdIn. Los puestos y preguntas se resuelven una vez por puesto distinto, reutilizando mapas; no hay lecturas por fila/pregunta de historial. El cálculo y los filtros son Java puro; la transacción de lectura mantiene un conjunto coherente.

### MCK10

/historial protegido con authGuard. Enlaces desde cabecera, panel, resumen y MCK09. Filtros etiquetados, aplicar/limpiar, estados vacíos y error reintentable. Fechas de filas UTC rotuladas Inicio/Fin; Sin promedio en no completas. Indicadores del backend, gráfico SVG accesible y tabla cronológica de fecha_fin ascendente; sesiones por inicio descendente, controles anterior/siguiente y total filtrado. Un punto no inventa variación; cero puntos muestra Sin datos. Navega al resumen, detalle, continuación y resultados individuales con video firmado renovable.
