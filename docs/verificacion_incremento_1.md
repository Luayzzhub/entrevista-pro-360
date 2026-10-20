# Verificación del Incremento 1

Fecha: 20 de octubre de 2026, America/Lima. Código disponible para revisión; **sin commits** y sin publicación remota.

## Resultados ejecutados

| Comprobación | Resultado observado |
| --- | --- |
| `mvn test` con H2 | **BUILD SUCCESS**: 17 casos registrados, 15 ejecutados sin fallos ni errores; 2 casos exclusivos de MySQL omitidos por condición de entorno |
| `mvn test -Dspring.profiles.active=mysql` | **BUILD SUCCESS**: 17 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas |
| Flyway en MySQL 8.0.44 | V1 y V2 aplicadas; ocho tablas de negocio, 68 columnas; tabla de historial técnica adicional |
| Verificación de metadatos MySQL | InnoDB, `utf8mb4_unicode_ci`, cuatro columnas JSON, ocho claves foráneas; cuatro borrados CASCADE, cuatro RESTRICT y ocho actualizaciones CASCADE |
| `mvn package -DskipTests` después de las pruebas | **BUILD SUCCESS**; JAR ejecutable generado por Spring Boot |
| JAR iniciado contra MySQL + HTTP real | `GET /api/v1/puestos`: tres puestos; consulta de preguntas de cada ID recibido: tres preguntas por puesto, órdenes 1, 2 y 3 |
| `npm install` y resolución del lock | Dependencias instaladas; versiones directas coinciden con `package.json` y `package-lock.json` |
| `npm run build` final | **Correcto**, compilación de producción; carga inicial 246,60 kB, transferencia estimada 67,06 kB; dentro del presupuesto de 500 kB |
| Navegador conectado por proxy a la API real | Tres puestos visibles; navegación a Soporte Técnico con sus tres preguntas y ayuda STAR en la segunda; regreso al catálogo; puesto inexistente muestra el mensaje de API y reintento |
| Revisión visual | Cabecera, portada, tipografía y distribución a una columna observadas en el ancho disponible del navegador; no sustituye una matriz completa de dispositivos |
| Importación `.env` | Script verificado con valor entre comillas y URL que contiene `&`; conserva los valores sin evaluarlos como código |
| Git | Repositorio local inicializado en rama `main`, sin commit; dependencias, compilados y herramientas temporales excluidos |

Entorno utilizado: JDK 17.0.12, Maven 3.9.9, Node 24.19.0, npm 11.17.0, MySQL 8.0.44, Spring Boot 3.5.7 y Angular 21.2.0. Maven se descargó para esta verificación en `.tools/apache-maven-3.9.9/`; no se instaló globalmente. Para usarlo en este mismo equipo, desde `backend/` puede sustituirse `mvn` por `..\.tools\apache-maven-3.9.9\bin\mvn.cmd` y configurar `JAVA_HOME` al JDK 17.

La verificación utilizó una instancia temporal en `127.0.0.1:33307` y la base `ep360_incremento1_test`, ubicada bajo `.tools/`. No se utilizaron credenciales ni bases personales. El JAR se probó en el puerto 18080 y la interfaz temporal en 14200; los puertos normales de arranque siguen siendo 8080 y 4200.

## Cobertura de las pruebas automatizadas

| Suite | Casos | Qué comprueba |
| --- | --- | --- |
| `PuestoPreguntaTest` | 6 | Exactamente tres preguntas, órdenes únicos 1/2/3, mismo puesto, actividad, entradas nulas, orden inválido y límite de 60 s |
| `LayerDependenciesTest` | 1 | Dominio y aplicación sin imports de Spring/JPA/clientes ni capas externas |
| `CatalogoIntegrationTest` | 8 | Catálogo activo, preguntas de cada puesto ordenadas, STAR y límite, 404 inexistente/inactivo, 409 incompleto, 400 de IDs inválidos, 401 privado y errores estándar 404/405 |
| `MySqlSchemaIntegrationTest` | 2 | Tablas/columnas/motor/collation y tipos JSON/acciones referenciales/Flyway sobre MySQL |

Las pruebas MVC usan el contexto completo de Spring, seguridad, casos de uso y JPA, sin mocks del catálogo. La comprobación HTTP adicional ejecutó el JAR con servidor embebido real.

## Evidencias conservadas

- [`evidencias/pruebas-backend.json`](evidencias/pruebas-backend.json): suites, nombres de casos y conteos, extraídos de los reportes Surefire del pase MySQL.
- [`evidencias/catalogo-http.json`](evidencias/catalogo-http.json): respuestas públicas obtenidas por HTTP del JAR conectado a MySQL, con tres puestos y nueve preguntas.
- [`evidencias/versiones-resueltas.json`](evidencias/versiones-resueltas.json): dependencias directas del POM efectivo y del lock npm.

Los reportes Surefire originales están en `backend/target/surefire-reports/`; logs de construcción y el POM efectivo, en `.tools/`. Ambos directorios están excluidos de Git. El JSON de evidencia contiene únicamente el catálogo público y resultados técnicos, sin secretos.

## Archivos construidos

| Ubicación | Contenido |
| --- | --- |
| Raíz | `.gitignore`, `.env.example`, `README.md` y repositorio Git inicializado |
| `backend/` | POM, aplicación, configuración, dos migraciones, ocho modelos canónicos, puertos/caso de uso, adaptadores JPA, API/DTO/errores, seguridad y cuatro suites de pruebas |
| `frontend/` | Configuración Angular/TypeScript, package/lock, proxy, estilos VIS-1.0, core HTTP, cabeceras pública/autenticada, catálogo y preguntas; carpetas restantes con alcance pendiente |
| `docs/` | Arquitectura, dependencias, API, matriz RF, este registro y tres evidencias JSON |
| `scripts/` | Importador seguro de variables `.env` para PowerShell |

Los documentos existentes del proyecto se mantuvieron sin cambios.

## Límites actuales

Se completa el alcance técnico del Incremento 1. RF-04/RF-06 se registran **En progreso** en la matriz de seguimiento solicitada; el flujo de sesión y bloqueo de avance todavía depende de incrementos futuros. RF-20 solo tiene el manejo y reintento de consultas del catálogo.

No están implementados registro/login/JWT, perfil, sesiones, captura, S3, FFmpeg/STT/LLM, evaluación, resultados ni historial. JOSE y SDK S3 están declarados por solicitud, con uso futuro expresamente identificado. No se validó disponibilidad de modelos ni credenciales externas. No se certifica 100 % del MVP. No se creó commit, remoto ni publicación GitHub.
