# Verificación del Incremento 9

Fecha: 27/10/2026, America/Lima. Estado: **AUDITADO Y APROBADO SIN OBSERVACIONES por el Orquestador**. RF-17, RF-18 y RF-19 **Verificados**; **20/20 RF verificados localmente**. Cierre y publicación autorizados en el Paso 1 del Incremento 10.

## Cierre previo del Incremento 8

El Orquestador lo declaró **AUDITADO Y APROBADO SIN OBSERVACIONES**, con RF-16 Verificado y RF-17 En progreso en ese corte. Antes de escribir código del9 se actualizaron el informe8, su JSON y la matriz RF; .tools siguió ignorado y fuera del índice.

- Commit atómico en main: **a05ca28d539eadb8af1b5a2c1b27e050fdc6fef3**.
- Mensaje exacto: `feat: implementar cierre atomico de sesion COMPLETADA, promedio global y resumen de sesion`.
- Push origin main correcto: ca798f5..a05ca28.
- HEAD, origin/main y git ls-remote origin refs/heads/main coincidieron en a05ca28.
- Antes del Paso2, git status --short --branch mostró únicamente **## main...origin/main**.

Ese HEAD corresponde al corte de verificación previo a la auditoría. Tras la aprobación del Orquestador, el Incremento 9 se cierra con un único commit atómico en main y push a origin/main; el hash publicado se registra en el informe del Incremento 10 para evitar una referencia circular en este commit.

## Resultado y fuentes

Historial propio paginado en /historial (MCK10), filtros D-10, indicadores y serie comparable PM-17. RF-17, RF-18 y RF-19 quedan **Verificados**, completando **20/20 RF implementados y comprobados en el alcance local**. El Orquestador declaró el Incremento 9 **AUDITADO Y APROBADO SIN OBSERVACIONES**.

Se leyeron las fuentes exactas externas al repositorio, sin modificarlas:

- Documentacion de work/WORK CHATGPT/Entrevista_Pro_360_Decisiones_para_Desarrollo.md, Sección10.
- prioridad-media/Correcciones_prioridad_media.md, PM-17.
- prioridad-media/Datos_historial_PM.json:87.50,75.00,79.17,83.33, datos ilustrativos.

La serie E2E usa esos mismos puntajes calculados por el pipeline real local; sus fechas son las del recorrido ejecutado, no las fechas ilustrativas del JSON. Los límites horarios documentales se comprueban con fechas controladas en integración.

## Dominio y consulta

RegistroHistorial comprueba COMPLETADA con promedio/fin y exactamente tres respuestas de la sesión, identidades y preguntas distintas, todas EVALUADO, cada una con evaluación COM-1.0 asociada. No añade versión a sesiones ni depende de preguntas activas para determinar la rúbrica histórica.

FiltroHistorial valida puesto positivo, page>=0, size1..50, COM-1.0 y rango desde<=hasta. Convierte LocalDate en America/Lima a inicio UTC inclusivo y día siguiente UTC exclusivo, siempre sobre fecha_inicio. El rango invertido devuelve400 DATOS_INVALIDOS con el mensaje exacto y ambos errores_campo.

CalculadoraEvolucionHistorial es Java puro y determinista. Requiere puesto concreto y COM-1.0; calcula sobre todas las sesiones filtradas antes de paginar. Serie por fecha_fin ASC,id_sesion ASC; tabla por fecha_inicio DESC,id_sesion DESC. Los promedios persistidos son la fuente del cálculo BigDecimal, escala2 HALF_UP.

| Serie | Media | Mejor | Última−primera | Última−anterior |
| --- | --- | --- | --- | --- |
| Sin completas | null | null | null | null |
| Una completa75.00 | 75.00 | 75.00 | null | null |
| 87.50,75.00,79.17,83.33 | **81.25** | **87.50** | **-4.17** | **+4.16** |

Una evaluación real de0.00 es válida: las pruebas distinguen ese resultado de la ausencia de datos. No se inventan ceros ni variaciones.

GestionarSesionService reutiliza la transacción de lectura y dos consultas por lotes: respuestas por IDs de sesiones y evaluaciones por IDs de respuestas. Cachea puesto y banco una vez por puesto distinto; no consulta evaluaciones por cada fila. Primero ensambla el resumen global real del usuario, luego filtra/calcula comparación/pagina. Los tres campos globales usados por MCK03 conservan su significado.

GET /api/v1/sesiones añade pagina,tamano_pagina,total_items_filtrados,total_paginas,aviso_filtro_rubrica,comparacion y puestos_historial. Items conserva sus13 campos y añade version_rubrica/motivo_cierre. El selector histórico adicional solo contiene puestos de sesiones propias; se une al catálogo en Angular, incluidos inactivos. No se exponen referencias privadas.

Página fuera de rango: items[]. Offset calculado en long para aceptar Integer.MAX_VALUE sin desbordamiento. Con rúbrica y estado incompatible la intersección es vacía y se conserva el aviso.

## MCK10 y navegación

- /historial protegido con authGuard; enlaces desde cabecera, MCK03, resumen y MCK09.
- Filtros con labels visibles; aplicar/limpiar reinician página; validación de rango en cliente y servidor; error reintentable y carga accesible.
- Aviso exacto D-10; combinación incompatible conserva ambos selectores y explica por qué no hay coincidencias.
- Comparación deshabilitada solicita puesto/rúbrica; sin completas muestra Sin datos; una completa muestra punto único y aviso explícito de que requiere2 para variación.
- SVG con role=img, aria-label, title, puntos, puntajes y fechas finales; tabla cronológica accesible complementaria. Sin librería gráfica adicional.
- Filas con Inicio/Fin UTC rotulados, puesto/área/nivel, avance y rúbrica; no completas muestran Sin promedio y CANCELADA su motivo seguro.
- Botones anterior/siguiente e indicador de páginas/total; resumen/detalle o continuación según estado.
- El resumen enlaza MCK09, cuyo video SigV4 conserva reproducción y renovación desde la cuenta propia.

Se inspeccionaron las capturas de escritorio1365x1000 y móvil390x844. La primera ejecución detectó desbordamiento por el tamaño mínimo del SVG en una celda grid; se corrigió min-width:0 del contenedor y se repitió **todo** el recorrido. El gráfico/tabla tienen desplazamiento horizontal interno en móvil y no desbordan la página. No se suprimieron errores de consola.

## Verificación ejecutada

| Verificación | Resultado |
| --- | --- |
| H2, mismas V1/V2 | **194 descubiertas,192 ejecutadas,2 exclusivas MySQL omitidas;0 fallos/errores** |
| MySQL8.4.13,127.0.0.1:33307 | **194 ejecutadas,0 omitidas/fallos/errores** |
| Nuevas pruebas de dominio | **12 correctas** |
| Nuevas pruebas de integración | **12 correctas en ambos motores** |
| LayerDependenciesTest | Correcto: domain/application sin frameworks/SDKs |
| Medios frontend | **6 correctas** |
| Angular AOT producción | Correcto, sin advertencias de presupuesto |
| Maven package/JAR | Correcto |
| Chrome Headless156.0.8078.47 | **9 escenarios;0 errores JavaScript y0 de consola** |
| Esquema consultado en MySQL | **8 tablas/68 columnas; sesiones7/evaluaciones14; Flyway1/2 correctas** |

Las suites completas se volvieron a ejecutar tras el ajuste final de mensajes de validación. Chrome se ejecutó contra el JAR empaquetado y Angular, sin interceptar la API de aplicación. Sus datos se crearon mediante endpoints autenticados y uploads reales:5 sesiones completas/15 respuestas,1 cancelada,1 EN_PROCESO/1 respuesta. Total16 extracciones ffmpeg,16 peticiones STT y16 evaluaciones LLM HTTP locales.

Las12 pruebas de integración cubren: contrato global sin filtros, desempates, borde Lima04:30Z/05:00Z, D-10/rúbrica mixta/faltante, estados incompatibles, comparación de4 con size2, aislamiento,0/1 sesiones, puesto/estado/rango,400 por tipos/rangos/rúbrica/fecha, puesto inactivo y motivos seguros. Las12 de dominio cubren además duplicados/respuesta ajena/no evaluada, desempate de fecha_fin, deshabilitación y HALF_UP.

Los9 escenarios Chrome registran datos persistidos, entrada desde panel, listado mixto, D-10 incompatible, PM-17 exacto, paginación/móvil, resumen→MCK09/video/renovación, cero/una completas, validación/limpieza y cabecera (algunas comprobaciones se agrupan en un mismo escenario).

## Comandos y evidencia reproducible

JDK: C:\Program Files\Java\jdk-17. Maven local3.9.9, npm.cmd. Las pruebas usan bases aisladas ep360_incremento9_test y ep360_incremento9_browser con MySQL8.4.13 en33307, nunca una cuenta de producción.

Desde backend, con JAVA_HOME y FFMPEG_BIN/FFPROBE_BIN configurados:

```powershell
..\.tools\apache-maven-3.9.9\bin\mvn.cmd '-Dmaven.repo.local=../.tools/m2' -B -ntp test
# TEST_DB_URL, TEST_DB_USERNAME, TEST_DB_PASSWORD para la base aislada:
..\.tools\apache-maven-3.9.9\bin\mvn.cmd '-Dmaven.repo.local=../.tools/m2' '-Dspring.profiles.active=mysql' -B -ntp test
..\.tools\apache-maven-3.9.9\bin\mvn.cmd '-Dmaven.repo.local=../.tools/m2' -B -ntp -DskipTests package
```

Desde frontend: npm.cmd run test:media y npm.cmd run build. El recorrido requiere JAR18080, Angular14200 con proxy/api, S3 HTTP19090 y STT/LLM19091 (fixture reutilizada scripts/pruebas/pipeline-resumen-http-local.cjs). Los helpers locales .tools/start-inc9-mysql.ps1 y .tools/preview-inc9.ps1 prepararon este entorno, con procesos ocultos y limpieza por PID propio.

```powershell
node scripts/pruebas/verificar-incremento-9-browser.mjs
python scripts/pruebas/registrar-evidencias-incremento9.py
```

El E2E genera un WebM real de2s con ffmpeg, usa Chrome portátil en .tools/chrome-for-testing/chrome-win64/chrome.exe (o EP360_CHROME_BIN) y el Playwright auxiliar ya disponible en .tools/browser-check. No altera package.json ni lock. La generación de evidencia comprueba logs, XML de suites, hash JAR, fórmula canónica, esquema y estado Git.

Archivos revisables:

- docs/evidencias/pruebas-incremento-9.json: resultados consolidados y suites MySQL.
- docs/evidencias/pruebas-browser-incremento-9.json:9 escenarios, tráfico real de navegador y serie canónica.
- scripts/pruebas/verificar-incremento-9-browser.mjs y registrar-evidencias-incremento9.py.
- Capturas locales ignoradas .tools/inc9-historial-comparable.png y inc9-historial-movil.png.
- Logs locales ignorados .tools/inc9-{h2,mysql,package,angular-build,media,browser}.log y inc9-schema.txt.

## Invariantes y límites de aceptación

V1/V2 intactas, sin V3+,8 tablas/68 columnas y ddl-auto=validate. backend/pom.xml y frontend/package.json/package-lock.json intactos. Cuatro capas conservadas. .tools ignorado y fuera del índice. La evidencia técnica describe el corte previo a auditoría; esta actualización registra su aprobación sin observaciones y el cierre autorizado del Incremento 9.

20/20 RF significa implementación y pruebas **locales** en el alcance documentado, no certificación de despliegue de producción. AWS IAM/bucket, cuentas/credenciales/modelos de OpenAI y hardware físico no se certifican mediante las fixtures. S3/STT/LLM tienen clientes reales y tráfico HTTP local; sus respuestas de prueba no equivalen a resultados de una cuenta de producción.
