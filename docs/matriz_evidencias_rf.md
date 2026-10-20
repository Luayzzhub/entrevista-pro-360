# Seguimiento funcional — Incremento 1

Fecha: 20/10/2026. Fuente: sección 13 de `Entrevista_Pro_360_Decisiones_para_Desarrollo.md`, conciliada con el informe maestro y la solicitud de Incremento 1. Estados permitidos en esta etapa: **En progreso** y **Pendiente**. Una carpeta o un modelo no acreditan implementación funcional.

| RF | Función | Estado actual | Evidencia real de este incremento | Pendiente para aceptación completa |
| --- | --- | --- | --- | --- |
| RF-01 | Registro | Pendiente | Modelo `Usuario`, esquema `usuarios`, carpeta `features/auth` | Alta, contraseñas y correo duplicado |
| RF-02 | Acceso y salida | Pendiente | Política de seguridad inicial y cabecera autenticada reutilizable | Login, JWT en memoria, expiración, cierre y rutas privadas |
| RF-03 | Perfil | Pendiente | Modelo/esquema y carpeta `features/profile` | Consulta y edición del perfil propio |
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

No se certifica 100 % del MVP. Las consultas de RF-04/RF-06 y el manejo de sus errores son la sección funcional construida. Los resultados ejecutados y sus límites se registran en `verificacion_incremento_1.md`.

## Criterios del tercer entregable

| Criterio | Evidencia disponible | Alcance pendiente |
| --- | --- | --- |
| Arquitectura inicial construida | Backend con cuatro capas, migraciones, consulta vertical y Angular | Resto de operaciones del MVP |
| Librerías explicadas | `dependencias.md`, POM, package.json y lock | Actualizar al integrar los siguientes módulos |
| Git/GitHub con avances reales | Repositorio local inicializado, cambios disponibles para revisión | Primer commit tras revisión; remoto y publicación no solicitados |
| Interfaces funcionales del alcance | Catálogo y preguntas conectados, estados de consulta | Identidad, perfil, práctica, análisis, resultados e historial |
