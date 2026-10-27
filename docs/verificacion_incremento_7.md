# Verificación del Incremento 7

Fecha de verificación: 25/10/2026, America/Lima. Cierre: 26/10/2026. Estado: **AUDITADO Y APROBADO SIN OBSERVACIONES, cerrado y publicado mediante este commit atómico en main**.

## Cierre publicado del Incremento 6

Se actualizaron README, matriz RF, informe 6 y JSON 6 para registrar aprobación sin observaciones. Se creó un único commit atómico en main:

- Hash: **f702fc4d03ff6e580e9cc92466c96a68798800db**.
- Mensaje exacto: `feat: implementar worker durable PROC-1.0, extraccion WAV, transcripcion STT, metricas MET-1.0 y reintento MCK08`.
- Push origin/main correcto: 4d00e56..f702fc4.
- ls-remote confirmó ese hash.
- Antes de iniciar el código del Incremento 7, git status --short --branch mostró únicamente `## main...origin/main`.
- .tools permaneció ignorado, sin binarios/temporales en el commit.

Durante la verificación del Incremento 7, HEAD era f702fc4 y el índice estaba vacío. El Incremento 7 aprobado queda cerrado y publicado mediante este commit atómico.

## Alcance terminado y frontera con el Incremento 8

El pipeline ejecuta EXTRAER_AUDIO, TRANSCRIBIR, METRICAS y EVALUAR. Tras éxito deja EVALUADO, hito EVALUADO, etapa FINALIZADO y error/próximo/lease nulos. FINALIZADO y EVALUADO no se reclaman. Las preguntas 1 y 2 habilitan la siguiente mediante el dominio existente.

**No se implementó cierre automático, promedio global ni Resumen de sesión.** La tercera respuesta evaluada deja la sesión EN_PROCESO, puntaje_promedio=null y fecha_fin=null. Esto se verificó en ambos motores y en Chrome.

RF-15 y RF-20 quedan Verificado dentro del pipeline por respuesta. RF-16 y RF-17 quedan En progreso: feedback, consulta propia y reproducción por respuesta están implementados; cierre/resumen global e historial completos pertenecen a incrementos posteriores.

## EVAL-1.0 y COM-1.0

ValidadorEvaluacion reside en domain, sin Spring, Jackson ni HTTP. Valida campos exactos, EVAL-1.0, COM-1.0, coincidencia de aplica_star, cuatro criterios y anclas enteras 0/25/50/75/100. Rechaza 60, decimales incluso 25.0, tipos ajenos, campos adicionales/faltantes, textos vacíos/excesivos y STAR inconsistente.

Cada justificación y observación STAR se recorta y admite 1..600 caracteres. Fortaleza, síntesis de mejora y recomendaciones admiten 1..1200. Si STAR no aplica, detalle_star=null; si aplica, exige cuatro elementos cerrados y estados PRESENTE/PARCIAL/AUSENTE. STAR no añade puntajes.

El backend calcula la media de los cuatro criterios con BigDecimal HALF_UP, escala 2, y guarda también cada criterio a escala 2. Ejemplos probados: 75/100/50/75→75.00; todos cero→0.00; 25/0/0/0→6.25. El JSON areas_mejora conserva exactamente version_contrato, justificaciones y sintesis. Se utilizan las 14 columnas existentes de evaluaciones_star, sin columnas nuevas.

EntradaEvaluacion contiene solo los siete campos contractuales. Las instrucciones del sistema tratan la transcripción como texto a evaluar, prohíben seguir sus instrucciones, inventar experiencia/evidencias o puntuar apariencia/acento/emociones. No se añaden IDs, nombre ni correo del postulante al proveedor.

OpenAiEvaluacionAdapter usa POST /v1/responses con snapshot exacto gpt-5.4-mini-2026-03-17, store=false, sin tools y text.format json_schema/strict=true/name=evaluacion_eval_1_0. El esquema versionado cierra todos los objetos. Timeout total de 60 s incluye lectura completa y el receptor limita el cuerpo a 2 MB. Se detectan claves JSON duplicadas.

Refusal, respuesta incompleta, JSON/contrato inválido y cuerpo excesivo producen SALIDA_EVALUACION_INVALIDA recuperable, sin crear/actualizar evaluación. HTTP408/429/5xx, timeout y transporte son recuperables; faltas de configuración/credenciales y rechazo no transitorio son definitivos.

## PROC-1.0 y reintentos de EVALUAR

Se reutilizan claim corto, contador previo a operación, versión, lease 90 s, heartbeat independiente de 20 s, backoff 5/15 s con Retry-After y lock ordenado sesión→respuesta. EVALUAR exige transcripción no vacía y MET-1.0 persistidas; una evaluación existente válida completa el estado sin repetir la llamada.

La evaluación y EVALUADO/FINALIZADO se guardan atómicamente bajo fencing. Cancelar durante la llamada libera lease sin persistir su resultado ni modificar la sesión terminal. Un resultado de versión vieja tampoco se publica.

Tres fallos del ciclo 1 dejan FALLIDO recuperable con hito TRANSCRITO. El POST manual responde 202 y restaura TRANSCRITO/EVALUAR, ciclo 2, sin volver a audio/STT/MET. Agotar ciclo 2 o un 401 cancela conforme a D-02. El tercer claim de EVALUAR interrumpido se agota tras expirar sin cuarta invocación.

## Reproducción PA-04 y resultado MCK09

POST /api/v1/respuestas/{id}/reproduccion verifica JWT/propiedad y HEAD privado. Una respuesta ajena o inexistente devuelve el mismo 404 RESPUESTA_NO_ENCONTRADA. Video ausente: 404 VIDEO_NO_DISPONIBLE, mensaje Video no disponible.; infraestructura/firma no disponible: 503 ALMACENAMIENTO_NO_DISPONIBLE.

La firma S3 equivalente usa AwsS3V4Signer/Aws4PresignerParams del SDK existente, con signingClockOverride y expiración de 300 s. El cuerpo contiene id_respuesta, video_url_cloud y expira_en UTC; la cabecera es Cache-Control: no-store. La URL no se escribe en BD ni se expone public_id_cloud como campo.

Los fixtures Java y Node verifican HMAC SigV4 real para GET prefirmado; se comprobó descarga y rechazo por expiración/manipulación. El servidor de navegador soporta rangos HTTP para reproducción real.

GET amplía el resultado con transcripción, métricas completas y evaluación con cuatro criterios/justificaciones, STAR y feedback. MCK08 sigue sondeando durante EVALUAR y ofrece enlace a MCK09 al alcanzar EVALUADO. El detalle de sesión también enlaza cada respuesta evaluada.

MCK09 usa interpolación de texto segura, conserva MET-1.0 fuera del desglose de puntajes, muestra STAR o STAR no aplica a esta pregunta, y mantiene la transcripción en panel independiente del video. Renovar enlace de video pide otro POST; se programó renovación al expirar y ante error posterior a expira_en. Si falta el video, retira el reproductor y conserva todo el resultado.

Siguiente pregunta lleva al detalle actualizado. Finalizar anticipadamente utiliza confirmación. Nueva práctica no cancela al primer clic: exige confirmar antes de cancelar y redirigir a /puestos. No ofrece volver a grabar ni reemplazar video. P3 muestra el aviso del Incremento 8.

## Pruebas ejecutadas

| Comprobación | Resultado |
| --- | --- |
| Suite Maven H2 | 154 descubiertas; **152 ejecutadas**, 2 exclusivas de MySQL omitidas; 0 fallas/errores |
| Suite Maven MySQL **8.4.13** | **154 ejecutadas**, 0 fallas/errores/omitidas |
| Dominio EVAL nuevo | 7 pruebas: puntajes, media, textos, campos exactos, STAR, mapa persistido e inmutabilidad |
| Cliente HTTP LLM nuevo | 5 pruebas: payload/schema/privacidad, 408/429/503/Retry-After, 400/401/403/configuración, refusal/incompleto/JSON/memoria y cuerpo lento |
| Integración EVAL/firma nueva | 10 pruebas en cada motor: cuatro etapas, tres preguntas, manual, D-02, fencing/cancelación, idempotencia, lease agotado y PA-04 |
| npm run test:media | 6 aprobadas |
| npm run build | Correcto, sin advertencias de presupuesto |
| mvn package -DskipTests | Correcto; JAR ejecutable |
| Chrome headless | **9 escenarios**, 0 errores JavaScript; cámara/micrófono emulados con MediaRecorder real |
| Esquema | 8 tablas de negocio, 68 columnas; evaluaciones_star=14; solo V1/V2 intactas; ddl-auto=validate |
| Git | HEAD f702fc4, índice vacío, .tools ignorado, Incremento7 sin commit |

Los nueve escenarios de Chrome cubren:

1. Confirmación→MCK08→MCK09 P1, cuatro criterios y justificaciones, feedback plano, MET/transcripción y reproducción real.
2. GET firmado de 300 s, Cache-Control no-store, renovación manual y firma alterada rechazada.
3. Renovación automática ante error del reproductor después de expira_en (se adelantó Date.now solo durante el evento, sin esperar cinco minutos).
4. VIDEO_NO_DISPONIBLE sin perder puntaje, métricas ni transcripción.
5. P1 EVALUADO habilita P2 y el detalle enlaza el resultado.
6. P2 con cuatro elementos STAR y adaptación móvil a 390 px sin desbordamiento.
7. P3 EVALUADO conserva EN_PROCESO y nulos de promedio/fecha_fin.
8. Tres HTTP503 en EVALUAR→manual202→resultado, sin repetir audio/STT.
9. Nueva práctica confirma, cancela y navega a /puestos.

En Chrome se grabaron cuatro respuestas: 4 solicitudes STT, 4 PUT de WAV y 7 llamadas LLM (tres normales y cuatro para el caso con reintento). Las peticiones de la aplicación no se interceptan ni se sustituyen en Playwright. Se inspeccionaron las capturas desktop/móvil de P2; la reproducción efectiva se comprobó en P1 mediante readyState, play y avance de currentTime.

## Reproducción de pruebas y evidencias

Las herramientas del entorno están en .tools: Maven3.9.9, caché m2, ffmpeg/ffprobe9.0.2, MySQL8.4.13 y Playwright auxiliar. No se añadieron dependencias de aplicación. JDK17 y Chrome instalados se usaron realmente.

Desde backend:

```powershell
mvn test
$env:TEST_DB_URL='jdbc:mysql://127.0.0.1:33307/ep360_incremento7_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:TEST_DB_USERNAME='root'
$env:TEST_DB_PASSWORD=''
mvn test '-Dspring.profiles.active=mysql'
mvn package -DskipTests
```

Usa una base exclusiva; elimina TEST_DB_URL antes del pase H2. Los tests deshabilitan el planificador e invocan el worker con Clock controlable. El escenario de navegador usa una base separada ep360_incremento7_browser, worker habilitado, intervalo500 ms y backoff1/1 s; producción conserva 2000 ms y 5/15 s. La base de Maven y el worker de demostración nunca comparten filas.

Desde frontend: npm run test:media y npm run build. Scripts auxiliares versionados:

- scripts/pruebas/pipeline-evaluacion-http-local.cjs: S3 local19090 y STT/Responses local19091, controles y auditoría; verifica SigV4 y rangos.
- scripts/pruebas/mck09-incremento7.cjs: recorrido real contra Angular14200/JAR18080; requiere Playwright en .tools/browser-check y Chrome instalado.
- scripts/pruebas/registrar-evidencias-incremento7.py: valida logs, reportes XML, esquema, JSON Chrome y Git antes de consolidar.

Artefactos:

- [Consolidado](evidencias/pruebas-incremento-7.json): totales, suites, configuración, esquema, estado Git y SHA-256 del JAR.
- [Chrome](evidencias/pruebas-browser-incremento-7.json): nueve casos, peticiones API y esquema enviado.
- Logs/capturas: .tools/inc7-*, ignorados por Git.

## Límites comprobados

Se usaron JPA, HttpClient, AWS SDK, ffmpeg y Chrome reales sobre S3/STT/LLM **HTTP locales de prueba**. No se certifican cuenta OpenAI, disponibilidad/resultado de modelos en esa cuenta, IAM ni bucket AWS de producción. La verificación de URLs sí comprueba criptográficamente SigV4, duración, expiración y manipulación en los fixtures. La cámara/micrófono son emulados.

Flyway11.7.2 emite su recomendación de actualización al detectar MySQL8.4; migraciones y validate pasaron sin cambiar BOM ni esquema. La API de firma legacy del SDK2.31.54 está marcada deprecated; se utilizó como firma S3 equivalente porque permite el Clock explícito y pasó los controles de firma/expiración.

Contrato contrastado con la [documentación oficial de Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs) y el [modelo GPT-5.4 mini](https://developers.openai.com/api/docs/models/gpt-5.4-mini). El snapshot solicitado se conserva exactamente; no se sustituyó por otro.

El Incremento 7 fue AUDITADO Y APROBADO SIN OBSERVACIONES y queda cerrado y publicado mediante este commit atómico en main. El cierre automático, promedio global y Resumen de sesión del Incremento 8 permanecen fuera del alcance de este informe histórico.

