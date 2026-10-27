# Seguimiento funcional — Incrementos 1 a 9

Fecha: 27/10/2026 (America/Lima). Fuente: contratos de los Incrementos 1 a 9. **Verificado** identifica funciones implementadas y comprobadas en el alcance descrito; **En progreso**, cobertura parcial; **Pendiente**, funcionalidad sin construir. Los proveedores HTTP locales no certifican cuentas ni servicios de producción.

| RF | Función | Estado actual | Evidencia real de este incremento | Pendiente para aceptación completa |
| --- | --- | --- | --- | --- |
| RF-01 | Registro | Verificado | `UsuarioTest`, `AuthPerfilIntegrationTest`, `RegistrarUsuarioService`, `UsuarioJpaAdapter`, `/registro` conectado a API | Revisión del orquestador; límites Unicode, asignación en servidor y 409 probados |
| RF-02 | Acceso y salida | Verificado | Login real, HS256/1800 s, cuenta validada por petición, estado en memoria, guards y cierre de sesión | Sin renovación automática ni revocación al cerrar sesión, conforme al contrato |
| RF-03 | Perfil | Verificado | GET/PATCH autenticados, identidad conservada, campos cerrados, correo readonly, formulario y mensajes | Revisión del orquestador; nombre/resumen y UTC comprobados |
| RF-04 | Catálogo de puestos | Verificado | V2 intacta, API pública, catálogo/preguntas Angular, inicio desde tarjetas/preguntas, aviso real de sesión pendiente; suites H2/MySQL y recorrido Chrome; Incremento 3 auditado y aprobado | Sin pendiente dentro del alcance de catálogo |
| RF-05 | Inicio de sesión de entrevista | Verificado | `GestionarSesionService`, `SesionJpaAdapter`, cuatro endpoints, bloqueo pesimista de usuario/sesión, propiedad desde JWT; dos POST HTTP concurrentes: 201 + 409 y una fila; cancelación idempotente y nueva práctica; Incremento 3 auditado y aprobado | Confirmación/procesamiento pertenecen a otros RF |
| RF-06 | Tres preguntas fijas | Verificado | Avance P1→P2→P3 y cierre COMPLETADA con tres resultados válidos; órdenes e identidades distintas; H2/MySQL/Chrome | Sin pendiente en el flujo de tres respuestas |
| RF-07 | Permisos de cámara/micrófono | Verificado | `getUserMedia` real en Chrome con dispositivos emulados, pistas live/enabled, preview muted, aviso S3/STT/IA, denegación real y fallos parciales/incompatibilidad; limpieza comprobada; Incremento 4 auditado y aprobado | No se certifica una matriz de hardware físico/navegadores |
| RF-08 | Captura de video | Verificado | Captura/reproducción nativas de Chrome y confirmación real desde la toma local, con formatos explícitos; Incremento 5 aprobado sin observaciones | Sin pendiente en captura |
| RF-09 | Límite de 60 segundos | Verificado | Temporizador de borde en cliente + ffprobe real en servidor: 1..60 s antes de redondear, pistas/códecs; corto/61 s rechazados, WebM streaming aceptado; Incremento 5 aprobado | La duración del servidor sigue siendo la autoridad |
| RF-10 | Revisión y repetición | Verificado | Revisión y descarte antes de confirmar; conservación de toma ante red/503; MCK09 nunca reemplaza un video confirmado | Sin pendiente dentro del alcance de revisión |
| RF-11 | Carga privada | Verificado | S3AlmacenamientoVideoAdapter con SDK AWS real sobre HTTP S3 local, If-None-Match *, HEAD/reconciliación, SHA-256, sin ACL pública, DELETE compensatorio; UI de progreso y reintento | Configurar/verificar bucket, credenciales e IAM reales para despliegue AWS; no se aprovisionó un bucket de producción |
| RF-12 | Metadatos | Verificado | PUT multipart idempotente, ffprobe, 10 componentes canónicos, PROC-1.0 exacto, asociación propia y transacción respuesta+EN_PROCESO; concurrencia HTTP y compensación en H2/MySQL | Sin pendiente dentro del alcance de metadatos |
| RF-13 | Transcripción | Verificado | ffmpeg real WAV PCM mono 16 kHz; HttpClient multipart oficial, snapshot exacto, es/json, sin identidad; texto trim persistido, confianza NULL; suites H2/MySQL 8.4 y Chrome | Validar cuenta/credenciales y disponibilidad del snapshot en OpenAI de producción |
| RF-14 | Métricas MET-1.0 | Verificado | CalculadorMetricas puro, NFKC, Unicode, HALF_UP, siete muletillas y locución sin puntuación; upsert único, resumen público; pruebas de dominio e integración | Sin pendiente en MET-1.0 dentro del alcance implementado |
| RF-15 | Evaluación EVAL-1.0/COM-1.0 | Verificado | Responses HTTP real, snapshot exacto, JSON Schema estricto; validación pura, cuatro anclas/pesos y puntaje backend; persistencia en 14 columnas; STAR condicional; H2/MySQL/Chrome | Validar credenciales/modelo en cuenta OpenAI de producción |
| RF-16 | Feedback y resumen | Verificado | MCK09 individual y Resumen de sesión: cierre atómico, media BigDecimal exacta de tres evaluaciones válidas, desglose y fechas UTC; CANCELADA sin promedio, resultados previos conservados; H2/MySQL/Chrome | Sin pendiente en feedback y resumen del alcance implementado |
| RF-17 | Consulta propia | Verificado | Historial, detalle/resumen/resultados propios, aislamiento JWT en tabla/serie; navegación MCK03→MCK10→resumen→MCK09 y video SigV4 renovable; H2/MySQL/Chrome | Sin pendiente dentro del alcance local; Incremento 9 auditado y aprobado sin observaciones |
| RF-18 | Historial | Verificado | MCK10 conectado, cinco filtros D-10, fechas Lima→UTC, orden inicio DESC/id DESC, paginación y estados incompatibles explícitos; puestos históricos; H2/MySQL/Chrome | Sin pendiente en historial local; Incremento 9 auditado y aprobado sin observaciones |
| RF-19 | Comparación | Verificado | CalculadoraEvolucionHistorial pura, serie fin ASC/id ASC, puesto+COM-1.0, indicadores antes de paginar; PM-17:81.25/87.50/-4.17/+4.16; cero/una completas, SVG/tabla accesibles; H2/MySQL/Chrome | Sin pendiente en comparación local; Incremento 9 auditado y aprobado sin observaciones |
| RF-20 | Errores y recuperación | Verificado | PROC-1.0 etapas 1–4, manual único, D-02 en P3 conservando P1/P2, cancelación concurrente frente a cierre; 409 al intentar cancelar COMPLETADA; H2/MySQL/Chrome | Proveedores e IAM de producción sin certificar |

**20/20 RF implementados y verificados mediante pruebas locales.** El Incremento 9 completa historial y comparación, auditado y aprobado sin observaciones por el Orquestador. Evidencias en verificacion_incremento_9.md y evidencias/pruebas-incremento-9.json. El Incremento 8 fue AUDITADO Y APROBADO SIN OBSERVACIONES y publicado como a05ca28; los informes anteriores conservan su alcance histórico. Esta cobertura no certifica cuentas/modelos de producción ni IAM/bucket AWS.

## Criterios del tercer entregable

| Criterio | Evidencia disponible | Alcance pendiente |
| --- | --- | --- |
| Arquitectura inicial construida | Backend con cuatro capas, migraciones, consulta vertical y Angular | Auditoría final y configuración/verificación de proveedores en producción |
| Librerías explicadas | `dependencias.md`, POM, package.json y lock | Sin nuevas dependencias en el Incremento 9 |
| Git/GitHub con avances reales | Incrementos 1 a 8 publicados; Incremento 9 auditado y aprobado sin observaciones; cierre autorizado en Paso 1 del Incremento 10 | Commit atómico y push del Incremento 9 autorizados; hash en evidencia del Incremento 10 |
| Interfaces funcionales del alcance | MCK03..MCK10 y Resumen de sesión conectados, historial, filtros y comparación | Incremento 9 auditado y aprobado sin observaciones |
