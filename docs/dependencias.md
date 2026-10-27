# Dependencias — Incrementos 1 a 10 (Tercer Entregable)

Las versiones del backend provienen de `backend/pom.xml` y del BOM `spring-boot-dependencies:3.5.7` resuelto por Maven. Las del frontend están fijadas sin rangos en `frontend/package.json` y resueltas en `frontend/package-lock.json`. No se atribuye uso funcional a dependencias reservadas.

## Backend

| Librería/Herramienta | Capa/Módulo | Versión declarada / efectiva | Propósito técnico | Componente/archivo real que la utiliza en este incremento |
| --- | --- | --- | --- | --- |
| Java JDK | Backend completo | release 17; JDK de verificación 17.0.12 | Records, tipos de negocio y ejecución JVM | `domain/*.java`, `pom.xml` |
| Maven | Construcción | 3.9.9 utilizado; mínimo 3.6.3 | Resolver dependencias, compilar, probar y empaquetar | `backend/pom.xml` |
| Spring Boot parent / Maven plugin | Composición/build | 3.5.7 | Autoconfiguración y JAR ejecutable | `EntrevistaPro360Application.java`, `pom.xml` |
| `spring-boot-starter-web` | Presentación | 3.5.7; Spring MVC/Web 6.2.12 | HTTP REST, despacho de solicitudes y JSON | `CatalogoController`, `AuthController`, `PerfilController`, `SesionController`, `RespuestaController`, handlers JSON |
| `spring-boot-starter-validation` | Presentación | 3.5.7; Hibernate Validator 8.0.3.Final | Validar DTO de identidad/perfil/sesión, IDs y parámetros | DTO de registro/perfil/sesión; controladores con `@Positive` |
| `spring-boot-starter-data-jpa` | Infraestructura | 3.5.7; Spring Data JPA 3.5.5; Hibernate ORM 6.6.33.Final | Catálogo, identidad, sesiones y pipeline, transacciones y PESSIMISTIC_WRITE | Adaptadores de catálogo/usuario/sesión y ProcesamientoJpaAdapter; ocho entidades/repositorios, locks de sesión y respuesta |
| `spring-boot-starter-security` | Infraestructura/presentación | 3.5.7; Spring Security 6.5.6 | Cadena stateless, autenticación JWT y hash BCrypt con costo 12 por defecto | `SecurityConfig`, `JwtAuthenticationFilter`, `BCryptPasswordHasher`, `SecurityExceptionHandler` |
| `spring-security-oauth2-jose` | Infraestructura / identidad | 6.5.6, gestionada por Boot | Emitir y verificar JWT HS256, issuer y vigencia exacta | `IdentidadConfig` (`NimbusJwtEncoder`, `NimbusJwtDecoder`), `JwtTokenProvider`, `JwtAuthenticationFilter` |
| `mysql-connector-j` | Infraestructura, runtime | 9.4.0, gestionada por Boot | JDBC hacia MySQL | application.yml, datasource y adaptadores JPA; servidor 8.4.13 verificado nuevamente en Incremento 10 |
| `flyway-core` | Infraestructura | 11.7.2, gestionada por Boot | Historial, validación y aplicación de migraciones | `db/migration/V1__esquema_inicial.sql`, `V2__catalogo_semilla.sql`, `application.yml` |
| `flyway-mysql` | Infraestructura | 11.7.2, gestionada por Boot | Soporte específico del motor MySQL para Flyway | Mismas migraciones al iniciar con MySQL |
| software.amazon.awssdk:s3 | Infraestructura de almacenamiento | 2.31.54, ya declarada | S3Client real, firma, PUT condicional, HEAD/reconciliación y DELETE | AlmacenamientoConfig, S3AlmacenamientoVideoAdapter y pruebas con HTTP local |
| ffprobe | Infraestructura multimedia | 9.0.2 portable verificado | Contenedor, códecs, pistas y duración real; fallback de paquetes WebM streaming | FfprobeInspectorMultimediaAdapter, BinarioMultimedia |
| ffmpeg | Infraestructura multimedia y tests | 9.0.2 portable verificado | Extraer WAV PCM s16le mono 16 kHz y generar fixtures | FfmpegExtractorAudioAdapter, BinarioMultimedia y MediaFixtures |
| Jackson Databind | Presentación, transitiva de Web | 2.19.2 | Serializar DTO y errores con nombres estables | DTO `ApiError`, `SecurityExceptionHandler`, estrategia `SNAKE_CASE` |
| `spring-boot-starter-test` | Pruebas | 3.5.7; JUnit Jupiter 5.12.2 | Integración Spring, MockMvc y aserciones | `CatalogoIntegrationTest`, `PuestoPreguntaTest`, `LayerDependenciesTest`, `MySqlSchemaIntegrationTest` |
| H2 | Pruebas únicamente | 2.3.232, gestionada por Boot | Base efímera en modo MySQL para `mvn test` sin servicios externos | `src/test/resources/application.yml`; usa las migraciones originales |
| MySQL Server | Persistencia externa | 8.4.13 utilizado en Incremento 10; 8.0.44 en informes históricos | Esquema real, restricciones, claims y pipeline | Perfil mysql y JAR con datadir/base aislados |

Los starters declaran su versión mediante el parent. Las transitivas listadas explican el uso efectivo; no se añaden dependencias explícitas redundantes para Jackson, Hibernate o JUnit. La tabla no es un inventario de todas las transitivas.

## Frontend

| Librería/Herramienta | Capa/Módulo | Versión declarada / instalada | Propósito técnico | Componente/archivo real que la utiliza en este incremento |
| --- | --- | --- | --- | --- |
| Node.js / npm | Construcción | Node 24.19.0; npm 11.17.0 utilizados | Instalación y ejecución de Angular CLI | `package.json`, `package-lock.json` |
| `@angular/core` | Aplicación | 21.2.0 | Componentes standalone, inyección y señales sin Zone.js | `app.config.ts`, `app.component.ts`, `HistorialComponent`, `ResumenSesionComponent`, `ResultadoRespuestaComponent` y resto de componentes |
| `@angular/common` | Core HTTP / presentación | 21.2.0 | HttpClient, interceptores, DatePipe UTC y DecimalPipe con dos decimales | `SesionService`, `HistorialComponent`, `ResumenSesionComponent`, `ResultadoRespuestaComponent`, servicios HTTP |
| `@angular/forms` | Identidad, perfil e historial | 21.2.0 | Formularios reactivos y FormsModule para filtros | `LoginComponent`, `RegistroComponent`, `PerfilComponent`, `HistorialComponent`, validadores compartidos |
| `@angular/compiler` | Compilación Angular | 21.2.0 | Compilación de plantillas | Plantillas de componentes, procesadas por CLI/compiler-cli |
| `@angular/platform-browser` | Arranque | 21.2.0 | Bootstrap de aplicación web | `src/main.ts` |
| `@angular/router` | Navegación | 21.2.0 | Rutas diferidas, parámetros, enlaces y títulos | `app.routes.ts`, `HistorialComponent`, `ResumenSesionComponent`, `ResultadoRespuestaComponent`, cabeceras y guards |
| `rxjs` | Consultas y estado | 7.8.2 | Streams HTTP, cancelación de solicitudes anteriores y reintento explícito | `HistorialComponent`, `ResumenSesionComponent`, `ResultadoRespuestaComponent`, servicios e interceptores |
| `tslib` | Runtime generado | 2.8.1 | Helpers compartidos de TypeScript | Compilación con `importHelpers: true` en `tsconfig.json` |
| `@angular/build` | Build/serve | 21.2.0 | Compilación de producción y servidor de desarrollo | Builders `application` y `dev-server` de `angular.json` |
| `@angular/cli` | Herramientas | 21.2.0 | Comandos `ng build` y `ng serve` | Scripts `build` y `start` de `package.json` |
| `@angular/compiler-cli` | Build | 21.2.0 | AOT y validación estricta de plantillas | `angularCompilerOptions` en `tsconfig.json`, `npm run build` |
| `typescript` | Build | 5.9.3 | Comprobación estricta de tipos y compilación | `tsconfig.json`, `tsconfig.app.json`, fuentes `.ts` |
| Arial / CSS | Compartido | Fuente de sistema; VIS-1.0 | Jerarquía, colores, adaptación móvil y foco | `src/styles.css`; sin framework CSS externo |
| `navigator.mediaDevices.getUserMedia`, `MediaStreamTrack` | Captura local | Web APIs nativas; comprobación de capacidad en ejecución | Permisos, cámara/micrófono activos, `ended` y liberación con `stop()` | `GrabacionMediaService.preparar`, `pistasActivas`, `liberarDispositivos` |
| `MediaRecorder` | Grabación local | Web API nativa; sin paquete npm | `isTypeSupported`, MIME explícito, fragmentos cada 250 ms, errores y parada | `formatos-media.ts`, `GrabacionMediaService.iniciar/detener/consolidar` |
| `performance.now`, `setTimeout`, Blob, object URLs y HTMLVideoElement | Temporizador/revisión local | Web APIs nativas | Reloj monotónico, plazo de 60 s, tamaño/Blob, reproducción y revocación | `GrabacionMediaService`, `CapturaRespuestaComponent` y su plantilla |
| `node:test`, `node:assert`, TypeScript existente | Pruebas locales | Node del entorno + TypeScript 5.9.3 | Seis tests de reglas puras, sin añadir runner/dependencias | `tests/media-captura.test.mjs`, `npm run test:media` |

`ReactiveFormsModule` está activo en las tres pantallas de formulario. Los validadores de contraseña cuentan puntos de código y bytes UTF-8 sin recortar espacios. No se instala un SDK OpenAI en el frontend ni se envían secretos de proveedores al navegador. El Bearer token propio se conserva únicamente en memoria.

## Integraciones activas y validación productiva pendiente

| Librería/Herramienta | Capa/Módulo | Versión declarada | Propósito técnico | Estado/archivo real |
| --- | --- | --- | --- | --- |
| Extracción de audio con FFmpeg | Infraestructura activa | 9.0.2 | Extraer WAV desde video confirmado | Worker durable y FfmpegExtractorAudioAdapter; fuera de confirmación |
| STT OpenAI | Infraestructura activa | gpt-4o-mini-transcribe-2025-12-15 exacto | Transcribir WAV en español por multipart | OpenAiTranscripcionAdapter probado con HTTP local real; cuenta/snapshot de producción sin verificar |
| LLM OpenAI | Infraestructura activa | gpt-5.4-mini-2026-03-17 exacto | Responses con JSON Schema EVAL-1.0, store=false, sin tools | OpenAiEvaluacionAdapter y LlmHttpFixture; protocolo HTTP local verificado, cuenta de producción pendiente |

## Consulta de versiones

El Incremento 7 tampoco añade dependencias Maven/npm. Reutiliza HttpClient/JDK17 y Jackson para Responses con detección de claves duplicadas, límite de 2 MB y timeout total 60 s. Activa EvaluacionJpaEntity, octavo mapeo JPA, sin cambiar el esquema. ValidadorEvaluacion depende solo de Java. La firma GET usa AwsS3V4Signer/Aws4PresignerParams del SDK 2.31.54 existente, con signingClockOverride para respetar Clock (API legacy marcada deprecated por el SDK, funcional y probada). Los fixtures Java/Node verifican HMAC SigV4, expiración y manipulación; eso no acredita IAM de producción. Angular reutiliza Router, HttpClient, signals, RxJS y pipes; el resultado es una ruta standalone diferida.

El Incremento 8 conserva backend/pom.xml, frontend/package.json y su lock sin cambios. Reutiliza BigDecimal/RoundingMode en domain, consultas Spring Data por colecciones en infraestructura y las transacciones/bloqueos ya existentes para cerrar con EVALUAR P3. Angular utiliza Router/authGuard, RxJS/toSignal, DatePipe/DecimalPipe y CancelarPracticaComponent existentes. Se preparó Chrome for Testing 156.0.8078.47 portátil bajo .tools porque la instalación anterior ya no estaba disponible; Playwright auxiliar sigue fuera de las dependencias de aplicación. La descarga se resolvió desde el manifiesto oficial https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json y no modifica el sistema.

El Incremento 6 no añade dependencias Maven/npm. Usa HttpClient/BodyPublishers multipart del JDK 17, ProcessBuilder, AudioSystem para validar WAV sin hardware, Normalizer/Pattern/BigDecimal en dominio, ScheduledExecutorService para heartbeat y @Scheduled para trabajo secuencial. Activa dos mapeos JPA existentes en el esquema (transcripciones y metricas_comunicacion). Los fixtures HTTP usan HttpServer del JDK. Playwright sigue aislado en .tools para Chrome headless.

Desde `backend/`, `mvn help:effective-pom` muestra las dependencias resueltas; `mvn dependency:tree` permite ampliar el inventario. Desde `frontend/`, `npm ls --depth=0` compara las dependencias instaladas con el manifiesto. Usa `npm ci` para reproducir el lock. Los requisitos de compatibilidad oficiales están enlazados en README.

No se añadieron dependencias Maven en el Incremento 2: se activó JOSE, ya declarado, y se añadió solamente `@angular/forms` al manifiesto npm. `UsuarioTest`, `AuthPerfilIntegrationTest` e `IdentidadConfigTest` amplían la verificación automatizada con las dependencias de prueba existentes.

El Incremento 3 no añade dependencias Maven/npm: reutiliza JPA (`@Lock`), Spring Transaction (`@Transactional`, READ_COMMITTED), Hibernate JSON, Web/TestRestTemplate para HTTP real y `java.util.concurrent` para la barrera/solicitudes de la prueba concurrente. `Clock` del JDK sigue inyectado en los casos de uso. Los nuevos consumidores son `GestionarSesionService`, `SesionJpaAdapter`, `SesionController`, `SesionEntrevistaTest` y `SesionIntegrationTest`. Frontend reutiliza RxJS/signals, HttpClient, Router y pipes de Angular. El SDK S3 se activó en el Incremento 5 y continúa activo en la entrega final.

El Incremento 4 no añade dependencias Maven/npm al proyecto ni modifica el lock. Usa signals, `DestroyRef`, `viewChild`, DecimalPipe y Web APIs del navegador. Las pruebas puras utilizan el compilador TypeScript ya instalado y el runner de Node. La verificación de navegador usa Chrome instalado y Playwright en `.tools/browser-check` (auxiliar ignorado, separado de las dependencias de la aplicación), con dispositivos emulados por Chromium y grabaciones reales. Las versiones utilizadas y sus resultados están en `evidencias/pruebas-incremento-4.json`. S3, ffprobe y proveedores de IA no se invocan desde la captura local.

El Incremento 5 activa el SDK S3 existente sin añadir dependencias: el cliente usa DefaultCredentialsProvider, región/bucket y endpoint opcional con forcePathStyle. El adaptador emite peticiones HTTP reales firmadas, condicionales y privadas, con tiempos de llamada acotados. ffprobe y ffmpeg portables están en .tools/ffmpeg (ignorados); las pruebas generan medios con ffmpeg y validan con ffprobe real. HttpServer del JDK implementa el endpoint local de las pruebas; no se sustituye S3Client ni el inspector. En Angular se usa HttpClient XHR con eventos UploadProgress y effect con limpieza de sondeo para MCK 08. La verificación local no certifica IAM/políticas de una cuenta AWS de producción.

## Historial y evolución: Incremento 9

Sin nuevas dependencias ni cambios en POM, package.json o lock. Domain/application reutilizan BigDecimal, RoundingMode, java.time (America/Lima→UTC) y colecciones. Infraestructura reutiliza consultas Spring Data por lotes; presentación usa RequestParam/DateTimeFormat existentes. Angular reutiliza FormsModule, RxJS/switchMap/toSignal, DatePipe/DecimalPipe y SVG nativo accesible, sin librería gráfica adicional. Chrome/Playwright y ffmpeg auxiliares permanecen en .tools ignorado. Pruebas: H2 192 ejecutadas/2 omitidas; MySQL8.4.13 194 ejecutadas; medios6; Chrome9 escenarios sin errores de consola/página.


## Integración final y consumidores del Incremento 10

No se modifica backend/pom.xml, frontend/package.json ni package-lock.json. `evidencias/versiones-resueltas.json` se regenera desde el POM efectivo recién resuelto y registra los JAR exactos de BOOT-INF/lib; para cada paquete directo frontend comprueba igualdad entre versión declarada, lock e instalación. Incluye @angular/forms21.2.0, anteriormente ausente del JSON histórico. Las transitivas runtime completas se enumeran en ese JSON; la tabla anterior explica los componentes principales y todas las dependencias directas.

| Herramienta o API | Versión exacta / naturaleza | Uso real hasta el10 |
| --- | --- | --- |
| JDK BigDecimal, RoundingMode, java.time y colecciones | Java17; sin librería Maven externa | `CalculadoraEvolucionHistorial`, `FiltroHistorial`, `SesionEntrevista`, `CalculadorMetricas`; HALF_UP escala2 y Lima→UTC |
| Angular FormsModule | @angular/forms21.2.0 | Filtros D-10 de `HistorialComponent`; los formularios de identidad/perfil conservan ReactiveFormsModule |
| SVG y HTML accesible | APIs nativas, sin paquete gráfico | `HistorialComponent`: gráfico PM-17, puntos y tabla cronológica |
| Chrome for Testing | 156.0.8078.47 | Recorrido de las12 interfaces con dispositivos emulados y MediaRecorder nativo |
| Playwright auxiliar | 1.64.1; solo .tools/browser-check | `recorrido-incremento10.mjs`; no dependencia de frontend ni runtime productivo |
| Python | 3.13.7 verificado localmente | `registrar-evidencias-incremento10.py`; JSON, hashes SHA256, XML y ZIP con stdlib |
| Node HTTP/fs/child_process/net | Node24.19.0 del entorno; stdlib | Runner integral, servidor AOT/proxy y fixtures S3/STT/LLM; sin paquetes nuevos |

`ResultadoRespuestaComponent` reutiliza HttpClient/RxJS para resultado y renovación, DatePipe/DecimalPipe y Router; `ResumenSesionComponent` usa RxJS/toSignal y la misma API privada; `HistorialComponent` usa FormsModule, switchMap/toSignal, Router y pipes. `CalculadoraEvolucionHistorial` sigue en domain puro, sin Angular ni Spring. Ninguna vista importa secretos de AWS/OpenAI. Las versiones y resultados recién verificados se registran en la evidencia10; la producción externa permanece sin certificar.
