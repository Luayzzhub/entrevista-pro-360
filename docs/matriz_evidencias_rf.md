# Seguimiento funcional — Incrementos 1 a 4

Fecha: 21/10/2026 (America/Lima). Fuente: contratos de los Incrementos 1 a 4, derivados del complemento de decisiones del proyecto. La documentación fuente se consulta fuera del repositorio. **Verificado** identifica funciones construidas y comprobadas en el alcance descrito; **En progreso** identifica cobertura parcial y **Pendiente** identifica funcionalidad aún sin construir. Una carpeta o un modelo no acreditan implementación funcional.

| RF | Función | Estado actual | Evidencia real de este incremento | Pendiente para aceptación completa |
| --- | --- | --- | --- | --- |
| RF-01 | Registro | Verificado | `UsuarioTest`, `AuthPerfilIntegrationTest`, `RegistrarUsuarioService`, `UsuarioJpaAdapter`, `/registro` conectado a API | Revisión del orquestador; límites Unicode, asignación en servidor y 409 probados |
| RF-02 | Acceso y salida | Verificado | Login real, HS256/1800 s, cuenta validada por petición, estado en memoria, guards y cierre de sesión | Sin renovación automática ni revocación al cerrar sesión, conforme al contrato |
| RF-03 | Perfil | Verificado | GET/PATCH autenticados, identidad conservada, campos cerrados, correo readonly, formulario y mensajes | Revisión del orquestador; nombre/resumen y UTC comprobados |
| RF-04 | Catálogo de puestos | Verificado | V2 intacta, API pública, catálogo/preguntas Angular, inicio desde tarjetas/preguntas, aviso real de sesión pendiente; suites H2/MySQL y recorrido Chrome; Incremento 3 auditado y aprobado | Sin pendiente dentro del alcance de catálogo |
| RF-05 | Inicio de sesión de entrevista | Verificado | `GestionarSesionService`, `SesionJpaAdapter`, cuatro endpoints, bloqueo pesimista de usuario/sesión, propiedad desde JWT; dos POST HTTP concurrentes: 201 + 409 y una fila; cancelación idempotente y nueva práctica; Incremento 3 auditado y aprobado | Confirmación/procesamiento pertenecen a otros RF |
| RF-06 | Tres preguntas fijas | Verificado | Tres preguntas ordenadas, progreso desde respuestas persistidas, SUBIDO bloquea y EVALUADO habilita 2/3; `SesionEntrevistaTest`, `SesionIntegrationTest` en H2/MySQL; vista real auditada y aprobada | Confirmación, evaluación y cierre automático en incrementos posteriores |
| RF-07 | Permisos de cámara/micrófono | Verificado | `getUserMedia` real en Chrome con dispositivos emulados, pistas live/enabled, preview muted, aviso S3/STT/IA, denegación real y fallos parciales/incompatibilidad; limpieza comprobada; Incremento 4 auditado y aprobado | No se certifica una matriz de hardware físico/navegadores |
| RF-08 | Captura de video | Verificado | MediaRecorder real con MIME explícito, fragmentos, imagen/audio reproducibles en Blob local; stop manual y errores/ended recuperables; seis tests puros y E2E | Confirmación/carga privada pertenecen al Incremento 5 |
| RF-09 | Límite de 60 segundos | En progreso | Reloj monotónico, contador y parada automática real a 60000.6 ms; duración local 1..60 s, bytes positivos <50 MB y rechazo de toma <1 s; límites comprobados | Validación del contenedor, pistas y duración con ffprobe en servidor (Incremento 5) |
| RF-10 | Revisión y repetición | Verificado | Reproductor local con imagen/audio, metadatos, descarte y URL revocada, segunda toma de la misma pregunta; SELECT COUNT(*) confirma 0 filas en respuestas_video | Confirmar respuesta sigue deshabilitado; endpoint multipart idempotente en Incremento 5 |
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

No se certifica 100 % del MVP. RF-01..RF-08 y RF-10 están verificados en el alcance descrito; RF-09 permanece en progreso por ffprobe pendiente. El Incremento 4 acredita captura/revisión local con dispositivos Chromium emulados y medios reales, sin guardar respuestas en BD ni invocar S3. Los fixtures de avance secuencial siguen confinados a las pruebas de backend. `verificacion_incremento_4.md` y `evidencias/pruebas-incremento-4.json` registran 71 pruebas ejecutadas en H2 (2 omitidas), 73 en MySQL, 6 unitarias de medios, build Angular y 17 escenarios de Chrome. Los informes anteriores conservan su alcance histórico.

## Criterios del tercer entregable

| Criterio | Evidencia disponible | Alcance pendiente |
| --- | --- | --- |
| Arquitectura inicial construida | Backend con cuatro capas, migraciones, consulta vertical y Angular | Resto de operaciones del MVP |
| Librerías explicadas | `dependencias.md`, POM, package.json y lock | Actualizar al integrar los siguientes módulos |
| Git/GitHub con avances reales | Incrementos 1 `c626aaf`, 2 `e47eeb6` y 3 `214c011` publicados; Incremento 4 auditado y aprobado, confirmado en este commit de main | Publicación mediante git push y comprobación ls-remote; hash/resultado en verificación del Incremento 5 |
| Interfaces funcionales del alcance | Catálogo/preguntas, identidad, panel, sesiones y MCK 05/06/07 locales; Chrome contra JAR y MySQL, con permisos/captura/revisión/repetición reales | Confirmación/carga, validación ffprobe, análisis, resultados e historial completo |
