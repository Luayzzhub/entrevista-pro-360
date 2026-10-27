# Verificación del Incremento 10 — Entrega Final del Tercer Entregable

Fecha: 27/10/2026, America/Lima.

**Estado:** Implementado, verificado localmente y **aprobado en auditoría final del tercer entregable**.

El Orquestador declaró el Incremento 10 **AUDITADO Y APROBADO SIN OBSERVACIONES**. Cierre documental y publicación final autorizados en main.

## Cierre del Incremento 9 antes del Paso 2

El Orquestador lo declaró **AUDITADO Y APROBADO SIN OBSERVACIONES**, RF-17/18/19 Verificados y20/20 RF locales. Se actualizaron informe9, JSON9 y matriz, y se creó un único commit atómico en main: **dcfb66ddc8503db435ac6233c47e1280f27b083a**, mensaje exacto `feat: implementar historial paginado D-10, comparacion evolutiva PM-17 y vista MCK10`. Push origin main: a05ca28→dcfb66d. Antes del Paso2 `git status --short --branch` mostró exclusivamente `## main...origin/main`; `.tools/` ignorado y fuera del índice. Al consolidar, HEAD, origin/main y ls-remote coinciden. El Paso2 no utiliza git add/commit.

## Resultado integral observado

| Verificación nueva | Resultado |
| --- | --- |
| Maven test H2 | 194 descubiertas,192 ejecutadas,2 exclusivas MySQL omitidas;0 fallos/errores |
| Maven test perfil mysql | 194 ejecutadas,0 omitidas/fallos/errores; MySQL8.4.13:33307 |
| LayerDependenciesTest | Verde en ambos motores; domain/application puros |
| Maven package | JAR ejecutable compilado y utilizado por Chrome |
| Angular | Build AOT producción servido desde dist; sin advertencias |
| Medios | 6 pruebas correctas |
| Chrome for Testing 156.0.8078.47 | 12 casos,20RF,14 endpoints,12 interfaces;0 errores JavaScript y0 consola |
| Esquema real | 8 tablas/68 columnas; Flyway1/2 correctas; ddl-auto=validate |
| Proveedores locales | 17 llamadas STT, 14 LLM; SDK S3 real; WAV pcm_s16le mono16000Hz verificado con ffprobe |
| Invariantes Git | V1/V2 iguales a c626aaf; sin V3+; POM/package/lock intactos; índice vacío |

El recorrido registra desde MCK02, inicia en MCK01, persiste perfil y recorre los3 puestos y sus9 preguntas. Comprueba sesión activa única con409 desde cliente HTTP sin producir errores de consola. Chrome usa dispositivos emulados y MediaRecorder nativo para grabar P1/P2/P3, temporizador real, repetición de P1 sin mutar BD y revisión/reproducción local. Cada respuesta atraviesa S3/ffprobe y las4 etapas persistidas, observadas en orden; MCK08 muestra los hitos y MCK09 los4 criterios con justificaciones, STAR solo en P2, MET-1.0, transcripción y video reproducible con SigV4 renovada y no-store. No se intercepta la API.

P3 cierra la primera sesión con87.50; otras3 sesiones reales por API comprueban medias75.00,79.17,83.33 (BigDecimal HALF_UP escala2) y completan la serie PM-17. Una quinta sesión graba P1, provoca3 fallos STT en P2, recupera con el único reintento manual y cancela conservando las2 evaluaciones, puntaje_promedio=null. Se rechaza un nuevo reintento con409. Los errores esperados se comprueban mediante cliente HTTP sin ocultar mensajes de consola de Chrome. D-02 definitivo, fencing, concurrencia, bordes1/60s, fechas04:30Z/05:00Z y cero/una completa tienen cobertura en las suites H2/MySQL y evidencia histórica9; no se afirma que todos esos bordes se reproduzcan otra vez en este recorrido Chrome.

MCK10 aplica puesto/fechas Lima/estado/COM-1.0; calcula PM-17 **81.25 /87.50 /-4.17 /+4.16** antes de paginar size2, conserva indicadores en página2, muestra Sin promedio para CANCELADA y explica la intersección incompatible D-10. Navega a resumen y resultado propios; otra cuenta recibe404 para los recursos y lista vacía. Cerrar sesión limpia estado en memoria, vuelve a login y bloquea navegación atrás/directa a historial. Móvil390x844 sin desbordamiento de página.

## Reproducción desde la raíz en PowerShell

```powershell
node scripts/pruebas/verificar-incremento-10-entregable.mjs
# Revalidar/consolidar los artefactos ya ejecutados, sin relanzar los servicios:
python scripts/pruebas/registrar-evidencias-incremento10.py
```

Prerrequisitos locales existentes en `.tools/`: Maven3.9.9/cache m2, MySQL8.4.13 con datadir inicializado, ffmpeg/ffprobe9.0.2, Chrome for Testing y Playwright auxiliar1.64.1; node_modules instalado con lock. JDK17 en JAVA_HOME o ruta local predeterminada. El runner usa bases aisladas ep360_incremento10_test y ep360_incremento10_browser; no elimina bases ni modifica cuentas de producción. Usa root local sin contraseña solo para este datadir de pruebas. Reutiliza MySQL33307 si ya está vivo y valida su versión; si lo inicia, lo apaga al terminar. Rechaza puertos ocupados18080/14200/19090/19091 y cierra únicamente procesos propios, ocultos. No instala dependencias ni toca POM/package/lock. EP360_CHROME_BIN y EP360_PYTHON_BIN permiten seleccionar ejecutables auxiliares.

El entorno restringido de Codex requirió ejecutar fuera del sandbox por bloqueo TCP10013; el primer intento interrumpido no constituye evidencia de pruebas. La ejecución final correcta queda en `.tools/inc10-ejecucion.json`, con tiempos/códigos/logs. En otros equipos hay que preparar los prerrequisitos: `.tools/` se excluye de Git por diseño. La base browser conserva sus datos propios para revisión; los correos únicos permiten volver a ejecutar. Los tests mysql usan otra base exclusiva.

## Paquete auditable

- `docs/evidencias/pruebas-incremento-10.json`: suites separadas por motor, trazabilidad20RF, esquema,9 commits completos, hashes de JAR/scripts/invariantes y todos los assets AOT.
- `docs/evidencias/pruebas-browser-incremento-10.json`: casos,12 interfaces,14 endpoints, tráfico real, etapas, sesiones y auditorías locales.
- `docs/evidencias/versiones-resueltas.json`: POM efectivo nuevo, librerías exactas del JAR y12 paquetes frontend comprobados contra lock/instalación.
- `docs/matriz_evidencias_rf.md`, `docs/arquitectura.md`, `docs/dependencias.md`, `README.md`: cierre de los4 criterios y alcance.
- `.tools/inc10-{h2,mysql,package,media,angular-build,browser,versiones}.log`, `inc10-h2-reports/`, `inc10-mysql-reports/`, `inc10-schema.txt`: logs/XML originales locales ignorados; cada XML tiene hash en el JSON.
- `.tools/inc10-resumen.png`, `inc10-historial.png`, `inc10-historial-movil.png`: capturas del build ejecutado.

## Límites técnicos

Verificación local con clientes S3/HTTP y binarios reales sobre fixtures S3/STT/LLM. No certifica bucket/IAM AWS, cuenta/modelos OpenAI de producción ni matriz de hardware físico. La firma GET se verifica criptográficamente por la fixture local y la política privada se prueba con clientes reales; no acredita permisos de una cuenta AWS. STT/LLM devuelven resultados controlados del fixture. La integración de producción sigue requiriendo configuración y validación externa. El Orquestador aprobó sin observaciones la auditoría final del Incremento10 y del tercer entregable.
