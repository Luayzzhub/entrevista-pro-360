# Seguimiento funcional — Incrementos 1 a 7

Fecha: 25/10/2026 (America/Lima). Fuente: contratos de los Incrementos 1 a 7. **Verificado** identifica funciones implementadas y comprobadas en el alcance descrito; **En progreso**, cobertura parcial; **Pendiente**, funcionalidad sin construir. Los proveedores HTTP locales no certifican cuentas ni servicios de producción.

| RF | Función | Estado actual | Evidencia real de este incremento | Pendiente para aceptación completa |
| --- | --- | --- | --- | --- |
| RF-01 | Registro | Verificado | `UsuarioTest`, `AuthPerfilIntegrationTest`, `RegistrarUsuarioService`, `UsuarioJpaAdapter`, `/registro` conectado a API | Revisión del orquestador; límites Unicode, asignación en servidor y 409 probados |
| RF-02 | Acceso y salida | Verificado | Login real, HS256/1800 s, cuenta validada por petición, estado en memoria, guards y cierre de sesión | Sin renovación automática ni revocación al cerrar sesión, conforme al contrato |
| RF-03 | Perfil | Verificado | GET/PATCH autenticados, identidad conservada, campos cerrados, correo readonly, formulario y mensajes | Revisión del orquestador; nombre/resumen y UTC comprobados |
| RF-04 | Catálogo de puestos | Verificado | V2 intacta, API pública, catálogo/preguntas Angular, inicio desde tarjetas/preguntas, aviso real de sesión pendiente; suites H2/MySQL y recorrido Chrome; Incremento 3 auditado y aprobado | Sin pendiente dentro del alcance de catálogo |
| RF-05 | Inicio de sesión de entrevista | Verificado | `GestionarSesionService`, `SesionJpaAdapter`, cuatro endpoints, bloqueo pesimista de usuario/sesión, propiedad desde JWT; dos POST HTTP concurrentes: 201 + 409 y una fila; cancelación idempotente y nueva práctica; Incremento 3 auditado y aprobado | Confirmación/procesamiento pertenecen a otros RF |
| RF-06 | Tres preguntas fijas | Verificado | EVALUADO habilita la siguiente pregunta; P1→P2→P3 verificado en H2/MySQL/Chrome; enlaces de resultado en detalle | Cierre y promedio global en Incremento 8 |
| RF-07 | Permisos de cámara/micrófono | Verificado | `getUserMedia` real en Chrome con dispositivos emulados, pistas live/enabled, preview muted, aviso S3/STT/IA, denegación real y fallos parciales/incompatibilidad; limpieza comprobada; Incremento 4 auditado y aprobado | No se certifica una matriz de hardware físico/navegadores |
| RF-08 | Captura de video | Verificado | Captura/reproducción nativas de Chrome y confirmación real desde la toma local, con formatos explícitos; Incremento 5 aprobado sin observaciones | Sin pendiente en captura |
| RF-09 | Límite de 60 segundos | Verificado | Temporizador de borde en cliente + ffprobe real en servidor: 1..60 s antes de redondear, pistas/códecs; corto/61 s rechazados, WebM streaming aceptado; Incremento 5 aprobado | La duración del servidor sigue siendo la autoridad |
| RF-10 | Revisión y repetición | Verificado | Revisión y descarte antes de confirmar; conservación de toma ante red/503; MCK09 nunca reemplaza un video confirmado | Sin pendiente dentro del alcance de revisión |
| RF-11 | Carga privada | Verificado | S3AlmacenamientoVideoAdapter con SDK AWS real sobre HTTP S3 local, If-None-Match *, HEAD/reconciliación, SHA-256, sin ACL pública, DELETE compensatorio; UI de progreso y reintento | Configurar/verificar bucket, credenciales e IAM reales para despliegue AWS; no se aprovisionó un bucket de producción |
| RF-12 | Metadatos | Verificado | PUT multipart idempotente, ffprobe, 10 componentes canónicos, PROC-1.0 exacto, asociación propia y transacción respuesta+EN_PROCESO; concurrencia HTTP y compensación en H2/MySQL | Sin pendiente dentro del alcance de metadatos |
| RF-13 | Transcripción | Verificado | ffmpeg real WAV PCM mono 16 kHz; HttpClient multipart oficial, snapshot exacto, es/json, sin identidad; texto trim persistido, confianza NULL; suites H2/MySQL 8.4 y Chrome | Validar cuenta/credenciales y disponibilidad del snapshot en OpenAI de producción |
| RF-14 | Métricas MET-1.0 | Verificado | CalculadorMetricas puro, NFKC, Unicode, HALF_UP, siete muletillas y locución sin puntuación; upsert único, resumen público; pruebas de dominio e integración | Sin pendiente en MET-1.0 dentro del alcance implementado |
| RF-15 | Evaluación EVAL-1.0/COM-1.0 | Verificado | Responses HTTP real, snapshot exacto, JSON Schema estricto; validación pura, cuatro anclas/pesos y puntaje backend; persistencia en 14 columnas; STAR condicional; H2/MySQL/Chrome | Validar credenciales/modelo en cuenta OpenAI de producción |
| RF-16 | Feedback y resumen | En progreso | MCK09 individual: puntaje, cuatro justificaciones, fortaleza, mejora, recomendaciones, STAR y MET descriptivas separados; texto plano seguro | Cierre automático, promedio global y Resumen de sesión en Incremento 8 |
| RF-17 | Consulta propia | En progreso | GET con transcripción, métricas y evaluación propias; 404 ajeno; PA-04 HEAD+GET SigV4/300 s, no-store, renovación y video ausente sin perder resultado | Consulta integral de sesiones/historial en incrementos posteriores |
| RF-18 | Historial | Pendiente | Resumen de sesiones propias para el panel; carpeta `features/history` | Pantalla de historial, filtros, paginación y aceptación integral |
| RF-19 | Comparación | Pendiente | Contrato documental conservado | Comparación de sesiones completas del mismo puesto/rúbrica |
| RF-20 | Errores y recuperación | Verificado | PROC-1.0 etapas 1–4 por respuesta: claim, heartbeat/fencing, Retry-After, tres intentos por ciclo, único manual desde EVALUAR sin repetir audio/STT/MET; salida inválida recuperable y D-02; H2/MySQL/Chrome | Verificación circunscrita al pipeline por respuesta; cierre global en Incremento 8 |

No se certifica el 100 % del MVP. El Incremento 7 procesa hasta EVALUADO/FINALIZADO y habilita la siguiente pregunta, conservando EN_PROCESO incluso tras la tercera evaluación. Evidencias en verificacion_incremento_7.md y evidencias/pruebas-incremento-7.json. El Incremento 6 fue aprobado sin observaciones y publicado como f702fc4; los informes anteriores conservan su alcance histórico.

## Criterios del tercer entregable

| Criterio | Evidencia disponible | Alcance pendiente |
| --- | --- | --- |
| Arquitectura inicial construida | Backend con cuatro capas, migraciones, consulta vertical y Angular | Resto de operaciones del MVP |
| Librerías explicadas | `dependencias.md`, POM, package.json y lock | Actualizar al integrar los siguientes módulos |
| Git/GitHub con avances reales | Incrementos 1 a 6 publicados; Incremento 7 AUDITADO Y APROBADO SIN OBSERVACIONES, cerrado y publicado mediante este commit atómico en main | Árbol limpio y sincronizado antes de iniciar Incremento 8 |
| Interfaces funcionales del alcance | MCK 03..MCK 09 conectados: captura, procesamiento, reintento, resultado individual y reproducción privada | Cierre global/resumen en 8; historial posterior |
