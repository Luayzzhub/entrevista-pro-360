# Dependencias hasta el Incremento 4

Las versiones del backend provienen de `backend/pom.xml` y del BOM `spring-boot-dependencies:3.5.7` resuelto por Maven. Las del frontend están fijadas sin rangos en `frontend/package.json` y resueltas en `frontend/package-lock.json`. No se atribuye uso funcional a dependencias reservadas.

## Backend

| Librería/Herramienta | Capa/Módulo | Versión declarada / efectiva | Propósito técnico | Componente/archivo real que la utiliza en este incremento |
| --- | --- | --- | --- | --- |
| Java JDK | Backend completo | release 17; JDK de verificación 17.0.12 | Records, tipos de negocio y ejecución JVM | `domain/*.java`, `pom.xml` |
| Maven | Construcción | 3.9.9 utilizado; mínimo 3.6.3 | Resolver dependencias, compilar, probar y empaquetar | `backend/pom.xml` |
| Spring Boot parent / Maven plugin | Composición/build | 3.5.7 | Autoconfiguración y JAR ejecutable | `EntrevistaPro360Application.java`, `pom.xml` |
| `spring-boot-starter-web` | Presentación | 3.5.7; Spring MVC/Web 6.2.12 | HTTP REST, despacho de solicitudes y JSON | `CatalogoController`, `GlobalExceptionHandler` |
| `spring-boot-starter-validation` | Presentación | 3.5.7; Hibernate Validator 8.0.3.Final | Validar el ID positivo | `CatalogoController.preguntas` con `@Positive` |
| `spring-boot-starter-data-jpa` | Infraestructura | 3.5.7; Spring Data JPA 3.5.5; Hibernate ORM 6.6.33.Final | Catálogo, identidad, sesiones, transacciones y bloqueo PESSIMISTIC_WRITE | `CatalogoJpaAdapter`, `UsuarioJpaAdapter`, `SesionJpaAdapter`, cinco entidades/repositorios; `findByIdForUpdate`, `findPropiaForUpdate` |
| `spring-boot-starter-security` | Infraestructura/presentación | 3.5.7; Spring Security 6.5.6 | Cadena stateless, autenticación JWT y hash BCrypt con costo 12 por defecto | `SecurityConfig`, `JwtAuthenticationFilter`, `BCryptPasswordHasher`, `SecurityExceptionHandler` |
| `spring-security-oauth2-jose` | Infraestructura / identidad | 6.5.6, gestionada por Boot | Emitir y verificar JWT HS256, issuer y vigencia exacta | `IdentidadConfig` (`NimbusJwtEncoder`, `NimbusJwtDecoder`), `JwtTokenProvider`, `JwtAuthenticationFilter` |
| `mysql-connector-j` | Infraestructura, runtime | 9.4.0, gestionada por Boot | JDBC hacia MySQL | `application.yml`, datasource y `CatalogoJpaAdapter`; probado con servidor 8.0.44 |
| `flyway-core` | Infraestructura | 11.7.2, gestionada por Boot | Historial, validación y aplicación de migraciones | `db/migration/V1__esquema_inicial.sql`, `V2__catalogo_semilla.sql`, `application.yml` |
| `flyway-mysql` | Infraestructura | 11.7.2, gestionada por Boot | Soporte específico del motor MySQL para Flyway | Mismas migraciones al iniciar con MySQL |
| `software.amazon.awssdk:s3` | Multimedia futura | 2.31.54, explícita en POM | SDK S3 v2 para almacenamiento privado y firma futura | Declarada por solicitud en `pom.xml`; integración **pendiente**, no hay llamadas ni recursos creados |
| Jackson Databind | Presentación, transitiva de Web | 2.19.2 | Serializar DTO y errores con nombres estables | DTO `ApiError`, `SecurityExceptionHandler`, estrategia `SNAKE_CASE` |
| `spring-boot-starter-test` | Pruebas | 3.5.7; JUnit Jupiter 5.12.2 | Integración Spring, MockMvc y aserciones | `CatalogoIntegrationTest`, `PuestoPreguntaTest`, `LayerDependenciesTest`, `MySqlSchemaIntegrationTest` |
| H2 | Pruebas únicamente | 2.3.232, gestionada por Boot | Base efímera en modo MySQL para `mvn test` sin servicios externos | `src/test/resources/application.yml`; usa las migraciones originales |
| MySQL Server | Persistencia externa | 8.0.44 utilizado | Ejecutar el esquema real, restricciones y catálogo | Perfil `mysql` de pruebas y consulta HTTP del JAR |

Los starters declaran su versión mediante el parent. Las transitivas listadas explican el uso efectivo; no se añaden dependencias explícitas redundantes para Jackson, Hibernate o JUnit. La tabla no es un inventario de todas las transitivas.

## Frontend

| Librería/Herramienta | Capa/Módulo | Versión declarada / instalada | Propósito técnico | Componente/archivo real que la utiliza en este incremento |
| --- | --- | --- | --- | --- |
| Node.js / npm | Construcción | Node 24.19.0; npm 11.17.0 utilizados | Instalación y ejecución de Angular CLI | `package.json`, `package-lock.json` |
| `@angular/core` | Aplicación | 21.2.0 | Componentes standalone, inyección y señales sin Zone.js | `app.config.ts`, `app.component.ts`, componentes de catálogo/preguntas |
| `@angular/common` | Core HTTP / presentación | 21.2.0 | HttpClient, interceptores, DatePipe UTC y DecimalPipe con dos decimales | `ApiClient`, `SesionService`, `SesionComponent`, `PanelComponent`, servicios de identidad |
| `@angular/forms` | Identidad y perfil | 21.2.0 | Formularios reactivos, validadores y mensajes por campo | `login.component.ts`, `registro.component.ts`, `perfil.component.ts`, `shared/usuario.validators.ts` |
| `@angular/compiler` | Compilación Angular | 21.2.0 | Compilación de plantillas | Plantillas de componentes, procesadas por CLI/compiler-cli |
| `@angular/platform-browser` | Arranque | 21.2.0 | Bootstrap de aplicación web | `src/main.ts` |
| `@angular/router` | Navegación | 21.2.0 | Rutas diferidas, parámetros, enlaces y títulos | `app.routes.ts`, `preguntas.component.ts`, `public-header.component.ts` |
| `rxjs` | Consultas y estado | 7.8.2 | Streams HTTP, cancelación de solicitudes anteriores y reintento explícito | `catalogo.service.ts`, `catalogo.component.ts`, `preguntas.component.ts`, interceptor |
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

## Herramientas y proveedores pendientes

| Librería/Herramienta | Capa/Módulo | Versión declarada | Propósito técnico | Estado/archivo real |
| --- | --- | --- | --- | --- |
| FFmpeg / ffprobe | Infraestructura multimedia | Pendiente de instalación/verificación | Extraer WAV e inspeccionar códecs, duración y pistas | Requisito descrito en README; sin invocación en Incremento 1 |
| STT OpenAI | Infraestructura futura | `gpt-4o-mini-transcribe-2025-12-15`, solicitado | Transcribir audio | `.env.example`, `application.yml`; disponibilidad e integración pendientes |
| LLM OpenAI | Infraestructura futura | `gpt-5.4-mini-2026-03-17`, solicitado | Evaluación EVAL-1.0 | `.env.example`, `application.yml`; disponibilidad e integración pendientes |

## Consulta de versiones

Desde `backend/`, `mvn help:effective-pom` muestra las dependencias resueltas; `mvn dependency:tree` permite ampliar el inventario. Desde `frontend/`, `npm ls --depth=0` compara las dependencias instaladas con el manifiesto. Usa `npm ci` para reproducir el lock. Los requisitos de compatibilidad oficiales están enlazados en README.

No se añadieron dependencias Maven en el Incremento 2: se activó JOSE, ya declarado, y se añadió solamente `@angular/forms` al manifiesto npm. `UsuarioTest`, `AuthPerfilIntegrationTest` e `IdentidadConfigTest` amplían la verificación automatizada con las dependencias de prueba existentes.

El Incremento 3 no añade dependencias Maven/npm: reutiliza JPA (`@Lock`), Spring Transaction (`@Transactional`, READ_COMMITTED), Hibernate JSON, Web/TestRestTemplate para HTTP real y `java.util.concurrent` para la barrera/solicitudes de la prueba concurrente. `Clock` del JDK sigue inyectado en los casos de uso. Los nuevos consumidores son `GestionarSesionService`, `SesionJpaAdapter`, `SesionController`, `SesionEntrevistaTest` y `SesionIntegrationTest`. Frontend reutiliza RxJS/signals, HttpClient, Router y pipes de Angular. SDK S3 permanece reservado.

El Incremento 4 no añade dependencias Maven/npm al proyecto ni modifica el lock. Usa signals, `DestroyRef`, `viewChild`, DecimalPipe y Web APIs del navegador. Las pruebas puras utilizan el compilador TypeScript ya instalado y el runner de Node. La verificación de navegador usa Chrome instalado y Playwright en `.tools/browser-check` (auxiliar ignorado, separado de las dependencias de la aplicación), con dispositivos emulados por Chromium y grabaciones reales. Las versiones utilizadas y sus resultados están en `evidencias/pruebas-incremento-4.json`. S3, ffprobe y proveedores de IA no se invocan desde la captura local.
