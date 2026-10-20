# Entrevista Pro 360 — Incremento 1

Base del MVP universitario: backend Spring Boot con cuatro capas, MySQL/Flyway y frontend Angular con VIS-1.0. Incluye consulta real de puestos y preguntas. La interfaz permite explorar el catálogo; autenticación, grabación, análisis, resultados e historial quedan para los siguientes incrementos.

## Requisitos

| Herramienta | Requisito del proyecto |
| --- | --- |
| Java JDK | 17 (compilación objetivo); JDK 21 también puede compilar para 17 |
| Maven | 3.6.3 o superior; verificación realizada con 3.9.9 |
| Node.js | 20.19+, 22.12+ o 24.x, según compatibilidad de Angular 21 |
| npm | Incluido con Node; usar `npm ci` con el lock entregado |
| Angular CLI | 21.2.0 local, instalado mediante `npm ci`; no requiere instalación global |
| MySQL | 8.0+ con una base exclusiva del proyecto y usuario con permisos para ejecutar migraciones |
| Git | Necesario para versionar; repositorio inicializado sin commits |
| FFmpeg y ffprobe | Necesarios en incrementos de multimedia, accesibles en PATH; no intervienen en este catálogo |

Compatibilidad de referencia: [Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html) y [Angular](https://angular.dev/reference/versions). Las versiones declaradas y el uso real están en [docs/dependencias.md](docs/dependencias.md).

## 1. Base MySQL

Entra a MySQL con una cuenta administradora y crea una base y usuario exclusivos. Sustituye la contraseña de ejemplo:

```sql
CREATE DATABASE entrevista_pro_360 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'entrevista'@'localhost' IDENTIFIED BY 'CAMBIAR_LOCALMENTE';
GRANT ALL PRIVILEGES ON entrevista_pro_360.* TO 'entrevista'@'localhost';
```

No importes V1/V2 manualmente. Flyway los ejecuta una sola vez al iniciar y valida sus checksums. Las ocho tablas de negocio suman 68 columnas; la tabla técnica de Flyway es adicional. No cambies una migración ya aplicada: agrega V3 cuando corresponda.

## 2. Configuración de entorno

Desde la raíz, en PowerShell:

```powershell
Copy-Item .env.example .env
# Editar .env con DB_URL, DB_USERNAME y DB_PASSWORD reales.
. .\scripts\Import-Env.ps1
```

Spring y Angular no cargan `.env` automáticamente. Este script importa variables al proceso actual sin ejecutar el contenido. En otro terminal hay que volver a cargarlas. Alternativamente, configura las variables en el entorno del IDE o del sistema de despliegue.

`DB_URL`, `DB_USERNAME` y `DB_PASSWORD` son las únicas variables necesarias para el catálogo. `.env.example` también conserva `JWT_SECRET`, `AWS_REGION`, `S3_BUCKET`, `OPENAI_API_KEY`, `STT_MODEL` y `LLM_MODEL` para los contratos futuros. No se han integrado ni validado servicios S3/IA. Las credenciales AWS se obtendrán del proveedor estándar de credenciales, nunca de Angular. No publiques `.env` ni claves.

## 3. Arranque del backend

```powershell
cd backend
mvn spring-boot:run
```

El backend escucha en `http://localhost:8080`. Si `java` no apunta al JDK correcto, configura `JAVA_HOME` a tu JDK 17 y añade su carpeta `bin` a PATH. En este equipo se detectó `C:\Program Files\Java\jdk-17`; el acceso directo Oracle de PATH no respondió correctamente durante la inspección.

Prueba la API en un segundo terminal:

```powershell
$puestos = Invoke-RestMethod http://localhost:8080/api/v1/puestos
$puestos | Format-Table id_puesto, nombre_puesto
$idPuesto = $puestos[0].id_puesto
Invoke-RestMethod "http://localhost:8080/api/v1/puestos/$idPuesto/preguntas"
```

No se requiere token para estas consultas. El resto de rutas está protegido; identidad todavía no está implementada. Consulta [el contrato HTTP](docs/api-catalogo.md).

## 4. Arranque del frontend

En otro terminal desde la raíz:

```powershell
cd frontend
npm ci
npm start
```

Abre [http://localhost:4200](http://localhost:4200). `proxy.conf.json` reenvía `/api/**` a `127.0.0.1:8080`. El backend debe estar iniciado para obtener datos. Puedes seleccionar cualquier puesto y leer sus tres preguntas; no hay datos de respaldo simulados. Si el servidor no responde, se muestra un error con reintento.

## 5. Pruebas y compilación

```powershell
cd backend
mvn test
mvn package
cd ..\frontend
npm run build
```

`mvn test` no requiere MySQL, AWS ni OpenAI: utiliza H2 de prueba y las mismas migraciones Flyway. Verifica reglas del dominio, capas, JPA y endpoints con seguridad. Las pruebas específicas de MySQL se omiten en este modo. H2 no certifica el comportamiento de todos los tipos de MySQL.

Para verificar contra MySQL, crea **otra base exclusiva de pruebas**, con el mismo charset y usuario autorizado. Las pruebas insertan y actualizan registros en transacciones revertidas; la base debe contener únicamente la semilla inicial. Desde `backend/`:

```powershell
$env:TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/entrevista_pro_360_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:TEST_DB_USERNAME='entrevista'
$env:TEST_DB_PASSWORD='TU_PASSWORD_LOCAL'
mvn test '-Dspring.profiles.active=mysql'
```

El JAR queda en `backend/target/entrevista-pro-360-0.1.0-SNAPSHOT.jar`. Carga las variables en el terminal de ejecución antes de `java -jar`. La compilación Angular queda en `frontend/dist/entrevista-pro-360/browser/`. En producción se requiere servir ese directorio con fallback a `index.html` y reverse proxy `/api` al backend. No despliegues el servidor de desarrollo.

## Documentación y alcance

- [Arquitectura y límites de capas](docs/arquitectura.md).
- [Dependencias exactas y uso real](docs/dependencias.md).
- [Matriz de RF-01 a RF-20](docs/matriz_evidencias_rf.md).
- [Resultados de verificación](docs/verificacion_incremento_1.md).

El repositorio contiene además los documentos de diseño originales. No hay commits ni remoto GitHub configurado por este incremento; no se presenta historial de avances inexistente. La inicialización y los archivos están listos para revisión antes del primer commit.
