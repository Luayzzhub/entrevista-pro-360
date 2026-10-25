# Seguimiento funcional — Incrementos 1 a 5

Fecha: 21/10/2026 (America/Lima). Fuente: contratos de los Incrementos 1 a 5, derivados del complemento de decisiones del proyecto. La documentación fuente se consulta fuera del repositorio. **Verificado** identifica funciones construidas y comprobadas en el alcance descrito; **En progreso** identifica cobertura parcial y **Pendiente** identifica funcionalidad aún sin construir. Una carpeta o un modelo no acreditan implementación funcional.

| RF | Función | Estado actual | Evidencia real de este incremento | Pendiente para aceptación completa |
| --- | --- | --- | --- | --- |
| RF-01 | Registro | Verificado | `UsuarioTest`, `AuthPerfilIntegrationTest`, `RegistrarUsuarioService`, `UsuarioJpaAdapter`, `/registro` conectado a API | Revisión del orquestador; límites Unicode, asignación en servidor y 409 probados |
| RF-02 | Acceso y salida | Verificado | Login real, HS256/1800 s, cuenta validada por petición, estado en memoria, guards y cierre de sesión | Sin renovación automática ni revocación al cerrar sesión, conforme al contrato |
| RF-03 | Perfil | Verificado | GET/PATCH autenticados, identidad conservada, campos cerrados, correo readonly, formulario y mensajes | Revisión del orquestador; nombre/resumen y UTC comprobados |
| RF-04 | Catálogo de puestos | Verificado | V2 intacta, API pública, catálogo/preguntas Angular, inicio desde tarjetas/preguntas, aviso real de sesión pendiente; suites H2/MySQL y recorrido Chrome; Incremento 3 auditado y aprobado | Sin pendiente dentro del alcance de catálogo |
| RF-05 | Inicio de sesión de entrevista | Verificado | `GestionarSesionService`, `SesionJpaAdapter`, cuatro endpoints, bloqueo pesimista de usuario/sesión, propiedad desde JWT; dos POST HTTP concurrentes: 201 + 409 y una fila; cancelación idempotente y nueva práctica; Incremento 3 auditado y aprobado | Confirmación/procesamiento pertenecen a otros RF |
| RF-06 | Tres preguntas fijas | Verificado | Catálogo y avance secuencial real; una respuesta SUBIDO bloquea las siguientes; consulta con enlace a MCK 08 | Evaluación y cierre automático en incrementos 6/7 |
| RF-07 | Permisos de cámara/micrófono | Verificado | `getUserMedia` real en Chrome con dispositivos emulados, pistas live/enabled, preview muted, aviso S3/STT/IA, denegación real y fallos parciales/incompatibilidad; limpieza comprobada; Incremento 4 auditado y aprobado | No se certifica una matriz de hardware físico/navegadores |
| RF-08 | Captura de video | Verificado | Captura/reproducción nativas de Chrome y confirmación real desde la toma local, con formatos explícitos | Auditoría del Incremento 5 |
| RF-09 | Límite de 60 segundos | Verificado | Temporizador de borde en cliente + ffprobe real en servidor: 1..60 s antes de redondear, pistas/códecs; corto/61 s rechazados, WebM streaming aceptado | Auditoría del Incremento 5; no se sustituye la duración de servidor por el contador local |
| RF-10 | Revisión y repetición | Verificado | Revisión y descarte locales, repetición antes de confirmar; progreso/bloqueo de acciones y conservación de toma ante red/503 | Procesamiento automático posterior a SUBIDO en incrementos siguientes |
| RF-11 | Carga privada | Verificado | S3AlmacenamientoVideoAdapter con SDK AWS real sobre HTTP S3 local, If-None-Match *, HEAD/reconciliación, SHA-256, sin ACL pública, DELETE compensatorio; UI de progreso y reintento | Configurar/verificar bucket, credenciales e IAM reales para despliegue AWS; no se aprovisionó un bucket de producción |
| RF-12 | Metadatos | Verificado | PUT multipart idempotente, ffprobe, 10 componentes canónicos, PROC-1.0 exacto, asociación propia y transacción respuesta+EN_PROCESO; concurrencia HTTP y compensación en H2/MySQL | Trabajador de etapas en Incremento 6 |
| RF-13 | Transcripción | Pendiente | Modelo `Transcripcion`, tabla y nombre de modelo STT | FFmpeg y llamada real a STT, errores y texto consultable |
| RF-14 | Métricas MET-1.0 | Pendiente | Modelo `MetricaComunicacion` y tabla | Cálculos reales y pruebas MET-1.0 |
| RF-15 | Evaluación EVAL-1.0/COM-1.0 | Pendiente | Modelo `EvaluacionSTAR` y tabla | Integración, validación de esquema, criterios y cálculo del total |
| RF-16 | Feedback y resumen | Pendiente | Tipos decimales y carpeta `features/results` | Justificaciones, recomendaciones y cierre con tres respuestas |
| RF-17 | Consulta propia | Pendiente | Sesiones y estado de respuestas propios con 404 indistinguible; sin claves internas por API | Consulta integral de resultados y reproducción firmada |
| RF-18 | Historial | Pendiente | Resumen de sesiones propias para el panel; carpeta `features/history` | Pantalla de historial, filtros, paginación y aceptación integral |
| RF-19 | Comparación | Pendiente | Contrato documental conservado | Comparación de sesiones completas del mismo puesto/rúbrica |
| RF-20 | Errores y recuperación | En progreso | Códigos multimedia/S3, toma conservada, confirmación idempotente, 503 recuperable, cancelación durante upload y compensación; errores genéricos sin trazas | Recuperación durable del trabajador y reintento manual en incrementos siguientes |

No se certifica el 100 % del MVP. RF-01..RF-12 están verificados dentro del alcance descrito: la respuesta confirmada queda SUBIDO sin ejecutar STT/LLM. La verificación del Incremento 5 contiene 101 pruebas H2 ejecutadas (2 omitidas), 103 MySQL, 6 unitarias de medios y 7 escenarios reales de Chrome con JAR/MySQL/S3 HTTP local/ffprobe. Los informes anteriores conservan su alcance histórico.

## Criterios del tercer entregable

| Criterio | Evidencia disponible | Alcance pendiente |
| --- | --- | --- |
| Arquitectura inicial construida | Backend con cuatro capas, migraciones, consulta vertical y Angular | Resto de operaciones del MVP |
| Librerías explicadas | `dependencias.md`, POM, package.json y lock | Actualizar al integrar los siguientes módulos |
| Git/GitHub con avances reales | Incrementos 1 a 4 publicados; Incremento 5 auditado y aprobado sin observaciones, cerrado en este commit atómico de main | Se comprueba publicación y árbol sincronizado antes de iniciar Incremento 6; hash en su informe |
| Interfaces funcionales del alcance | MCK 03..MCK 08 conectados: captura, revisión, confirmación/progreso/reintento y consulta de estado | Extracción/STT/métricas en Incremento 6, evaluación en 7; resultados e historial posteriores |
