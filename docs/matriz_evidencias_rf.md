# Seguimiento funcional — Incrementos 1 y 2

Fecha: 20/10/2026. Fuente: contratos de los Incrementos 1 y 2, derivados del complemento de decisiones del proyecto. La documentación fuente se consulta fuera del repositorio. **Verificado** identifica funciones construidas y comprobadas; **En progreso** identifica cobertura parcial y **Pendiente** identifica funcionalidad aún sin construir. Una carpeta o un modelo no acreditan implementación funcional.

| RF | Función | Estado actual | Evidencia real de este incremento | Pendiente para aceptación completa |
| --- | --- | --- | --- | --- |
| RF-01 | Registro | Verificado | `UsuarioTest`, `AuthPerfilIntegrationTest`, `RegistrarUsuarioService`, `UsuarioJpaAdapter`, `/registro` conectado a API | Revisión del orquestador; límites Unicode, asignación en servidor y 409 probados |
| RF-02 | Acceso y salida | Verificado | Login real, HS256/1800 s, cuenta validada por petición, estado en memoria, guards y cierre de sesión | Sin renovación automática ni revocación al cerrar sesión, conforme al contrato |
| RF-03 | Perfil | Verificado | GET/PATCH autenticados, identidad conservada, campos cerrados, correo readonly, formulario y mensajes | Revisión del orquestador; nombre/resumen y UTC comprobados |
| RF-04 | Catálogo de puestos | En progreso | V2, `ConsultarCatalogoService`, `CatalogoController`, `CatalogoService`, vista Angular y pruebas de integración | Validación de aceptación del incremento por el equipo |
| RF-05 | Inicio de sesión de entrevista | Pendiente | Modelo `SesionEntrevista`, tabla de sesiones | Crear/continuar/cancelar práctica y unicidad de sesión no terminal |
| RF-06 | Tres preguntas fijas | En progreso | Semilla por nombre, regla `Puesto.validarPreguntasActivas`, API ordenada, vista Angular y pruebas | Avance secuencial vinculado a respuestas `EVALUADO` al implementar las sesiones |
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
| RF-17 | Consulta propia | Pendiente | Seguridad por defecto de rutas privadas | Autorización por propietario y reproducción firmada |
| RF-18 | Historial | Pendiente | Modelo de sesión y carpeta `features/history` | Consulta, filtros, paginación y estados vacíos |
| RF-19 | Comparación | Pendiente | Contrato documental conservado | Comparación de sesiones completas del mismo puesto/rúbrica |
| RF-20 | Errores y recuperación | En progreso | `GlobalExceptionHandler`, errores de seguridad, interceptor Angular y reintento de consultas | Recuperación durable del procesamiento, ciclo manual y cancelación definitiva |

No se certifica 100 % del MVP. RF-01/RF-02/RF-03 y las consultas de RF-04/RF-06 forman el alcance construido. La gestión de prácticas y el resto del procesamiento continúan pendientes. Los resultados y límites se registran en `verificacion_incremento_1.md` y `verificacion_incremento_2.md`.

## Criterios del tercer entregable

| Criterio | Evidencia disponible | Alcance pendiente |
| --- | --- | --- |
| Arquitectura inicial construida | Backend con cuatro capas, migraciones, consulta vertical y Angular | Resto de operaciones del MVP |
| Librerías explicadas | `dependencias.md`, POM, package.json y lock | Actualizar al integrar los siguientes módulos |
| Git/GitHub con avances reales | Incremento 1 en commit `c626aaf`; Incremento 2 en árbol de trabajo | GitHub CLI no disponible; publicación pendiente. Incremento 2 sin commit por solicitud |
| Interfaces funcionales del alcance | Catálogo/preguntas, registro, login, perfil y panel inicial conectados | Práctica, análisis, resultados e historial |
