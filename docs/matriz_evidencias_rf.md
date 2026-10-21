# Seguimiento funcional — Incrementos 1 a 3

Fecha: 20/10/2026 (America/Lima). Fuente: contratos de los Incrementos 1 a 3, derivados del complemento de decisiones del proyecto. La documentación fuente se consulta fuera del repositorio. **Verificado** identifica funciones construidas y comprobadas en el alcance descrito; **En progreso** identifica cobertura parcial y **Pendiente** identifica funcionalidad aún sin construir. Una carpeta o un modelo no acreditan implementación funcional.

| RF | Función | Estado actual | Evidencia real de este incremento | Pendiente para aceptación completa |
| --- | --- | --- | --- | --- |
| RF-01 | Registro | Verificado | `UsuarioTest`, `AuthPerfilIntegrationTest`, `RegistrarUsuarioService`, `UsuarioJpaAdapter`, `/registro` conectado a API | Revisión del orquestador; límites Unicode, asignación en servidor y 409 probados |
| RF-02 | Acceso y salida | Verificado | Login real, HS256/1800 s, cuenta validada por petición, estado en memoria, guards y cierre de sesión | Sin renovación automática ni revocación al cerrar sesión, conforme al contrato |
| RF-03 | Perfil | Verificado | GET/PATCH autenticados, identidad conservada, campos cerrados, correo readonly, formulario y mensajes | Revisión del orquestador; nombre/resumen y UTC comprobados |
| RF-04 | Catálogo de puestos | Verificado | V2 intacta, API pública, catálogo/preguntas Angular, inicio desde tarjetas/preguntas, aviso real de sesión pendiente; suites H2/MySQL y recorrido Chrome; Incremento 3 auditado y aprobado | Captura de video en RF-07..RF-10 (Incremento 4) |
| RF-05 | Inicio de sesión de entrevista | Verificado | `GestionarSesionService`, `SesionJpaAdapter`, cuatro endpoints, bloqueo pesimista de usuario/sesión, propiedad desde JWT; dos POST HTTP concurrentes: 201 + 409 y una fila; cancelación idempotente y nueva práctica; Incremento 3 auditado y aprobado | Captura/confirmación/procesamiento pertenecen a otros RF |
| RF-06 | Tres preguntas fijas | Verificado | Tres preguntas ordenadas, progreso desde respuestas persistidas, SUBIDO bloquea y EVALUADO habilita 2/3; `SesionEntrevistaTest`, `SesionIntegrationTest` en H2/MySQL; vista real auditada y aprobada | RF-07..RF-10 en Incremento 4; evaluación y cierre automático en incrementos posteriores |
| RF-07 | Permisos de cámara/micrófono | Pendiente | Alcance documentado | Permisos, denegación e incompatibilidad del navegador |
| RF-08 | Captura de video | Pendiente | Modelo `RespuestaVideo` | Captura real con imagen y audio |
| RF-09 | Límite de 60 segundos | Pendiente | Límite de pregunta en V2, dominio y DTO | Temporizador y validación real con ffprobe |
| RF-10 | Revisión y repetición | Pendiente | Alcance documentado | Reproducir, descartar y repetir antes de confirmar |
| RF-11 | Carga privada | Pendiente | SDK S3 declarado y variables de configuración | Adaptador, bucket privado, progreso y fallos de carga |
| RF-12 | Metadatos | Pendiente | Tabla `respuestas_video` con campos y restricciones exactos | Confirmación idempotente, validación y asociaciones |
| RF-13 | Transcripción | Pendiente | Modelo `Transcripcion`, tabla y nombre de modelo STT | FFmpeg y llamada real a STT, errores y texto consultable |
| RF-14 | Métricas MET-1.0 | Pendiente | Modelo `MetricaComunicacion` y tabla | Cálculos reales y pruebas MET-1.0 |
| RF-15 | Evaluación EVAL-1.0/COM-1.0 | Pendiente | Modelo `EvaluacionSTAR` y tabla | Integración, validación de esquema, criterios y cálculo del total |
| RF-16 | Feedback y resumen | Pendiente | Tipos decimales y carpeta `features/results` | Justificaciones, recomendaciones y cierre con tres respuestas |
| RF-17 | Consulta propia | Pendiente | Sesiones propias con JWT y 404 indistinguible para sesión ajena/inexistente, probado en Incremento 3 | Consulta integral de resultados y reproducción privada firmada |
| RF-18 | Historial | Pendiente | Resumen de sesiones propias para el panel; carpeta `features/history` | Pantalla de historial, filtros, paginación y aceptación integral |
| RF-19 | Comparación | Pendiente | Contrato documental conservado | Comparación de sesiones completas del mismo puesto/rúbrica |
| RF-20 | Errores y recuperación | En progreso | `GlobalExceptionHandler`, errores de seguridad/sesión, interceptor, reintentos y cancelación anticipada idempotente con confirmación | Recuperación durable del procesamiento, ciclo manual y cancelación por fallo definitivo |

No se certifica 100 % del MVP. RF-01 a RF-06 constituyen el alcance construido y verificado de estos incrementos. RF-07..RF-10 (cámara/micrófono, captura, límite y revisión) corresponden al Incremento 4. Las respuestas empleadas para comprobar el avance son fixtures persistidos exclusivamente en las pruebas; la aplicación no inventa respuestas ni evaluaciones. Los resultados/límites se registran en `verificacion_incremento_1.md`, `verificacion_incremento_2.md` y `verificacion_incremento_3.md`; este último incluye 71 pruebas ejecutadas en H2, 73 en MySQL, build Angular y recorrido de navegador.

## Criterios del tercer entregable

| Criterio | Evidencia disponible | Alcance pendiente |
| --- | --- | --- |
| Arquitectura inicial construida | Backend con cuatro capas, migraciones, consulta vertical y Angular | Resto de operaciones del MVP |
| Librerías explicadas | `dependencias.md`, POM, package.json y lock | Actualizar al integrar los siguientes módulos |
| Git/GitHub con avances reales | Incrementos 1 `c626aaf` y 2 `e47eeb6` publicados; Incremento 3 auditado/aprobado por el Orquestador y confirmado en este commit de `main` | Publicación del cierre mediante `git push origin main`; el resultado se registra al iniciar el Incremento 4 |
| Interfaces funcionales del alcance | Catálogo/preguntas, registro, login, perfil, panel y sesión conectados; creación/continuidad/cancelación verificadas en Chrome contra JAR y MySQL | Captura (Incremento 4), análisis, resultados e historial completo |
