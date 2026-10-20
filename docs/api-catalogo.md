# API de catálogo — Incremento 1

Base: `/api/v1`. JSON UTF-8, nombres en `snake_case`. Ambos GET son públicos y devuelven arrays, sin envoltura ni paginación.

## GET /puestos

200: puestos activos ordenados por `id_puesto`. La semilla contiene los tres puestos del MVP. Si no hay puestos activos, devuelve `[]`.

```json
[
  {
    "id_puesto": 1,
    "nombre_puesto": "Practicante de Desarrollo TI",
    "area": "Tecnología de la información",
    "descripcion": "Práctica orientada al desarrollo de software y la resolución de problemas técnicos.",
    "nivel_experiencia": "Practicante",
    "activo": true
  }
]
```

El ejemplo muestra un elemento abreviado del catálogo; los identificadores los asigna MySQL y no son constantes del cliente.

## GET /puestos/{id}/preguntas

200: exactamente tres preguntas activas del mismo puesto, en orden 1, 2 y 3.

```json
[
  {
    "id_pregunta": 1,
    "id_puesto": 1,
    "enunciado": "Preséntate brevemente y explica qué conocimientos o proyectos de tu formación puedes aportar como practicante de desarrollo.",
    "competencia_evaluada": "Presentación y pertinencia profesional",
    "orden": 1,
    "tiempo_limite_seg": 60,
    "activo": true,
    "aplica_star": false
  }
]
```

El ejemplo muestra una de las tres preguntas. STAR aplica solo a la segunda en cada puesto inicial. El endpoint es una consulta; no crea sesiones ni habilita grabación.

## Errores

Siempre contienen las tres propiedades siguientes; sin errores de campo, `errores_campo` es `{}`.

```json
{
  "codigo": "PUESTO_NO_ENCONTRADO",
  "mensaje": "El puesto no existe o no está activo.",
  "errores_campo": {}
}
```

| HTTP | Código | Caso |
| --- | --- | --- |
| 400 | `PARAMETRO_INVALIDO` | ID no numérico, no positivo o fuera del rango INT; `errores_campo.id` contiene ayuda |
| 404 | `PUESTO_NO_ENCONTRADO` | Puesto inexistente o inactivo |
| 409 | `CATALOGO_INVALIDO` | Puesto sin exactamente tres preguntas activas válidas |
| 401 | `NO_AUTENTICADO` | Ruta privada sin autenticación |
| 403 | `ACCESO_DENEGADO` | Acceso rechazado por seguridad |
| 404/405/406 | `SOLICITUD_INVALIDA` | Ruta pública inexistente, método no admitido o negociación inválida |
| 500 | `ERROR_INTERNO` | Fallo inesperado; la respuesta no expone excepciones internas |

No hay endpoints de escritura implementados. `/auth/**` está reservado y permitido por la política, pero una ruta aún inexistente devuelve 404.
