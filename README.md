# Entrevista Pro 360 — Incrementos 1 y 2

Base del MVP universitario: backend Spring Boot de cuatro capas, MySQL/Flyway y Angular VIS-1.0. Incluye catálogo, registro, acceso JWT, salida, perfil básico y panel inicial. Grabación, sesiones de práctica, análisis, resultados e historial corresponden a los siguientes incrementos.

## Requisitos

| Herramienta | Requisito del proyecto |
| --- | --- |
| Java JDK | 17 (compilación objetivo); JDK 21 también puede compilar para 17 |
| Maven | 3.6.3 o superior; verificación realizada con 3.9.9 |
| Node.js | 20.19+, 22.12+ o 24.x, según compatibilidad de Angular 21 |
| npm | Incluido con Node; usar `npm ci` con el lock entregado |
| Angular CLI | 21.2.0 local, instalado mediante `npm ci`; no requiere instalación global |
| MySQL | 8.0+ con una base exclusiva del proyecto y usuario con permisos para ejecutar migraciones |
| Git | Incremento 1 confirmado en `c626aaf`; Incremento 2 sin commit para revisión |
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

`DB_URL`, `DB_USERNAME` y `DB_PASSWORD` conectan la base. `JWT_SECRET` configura HS256 y debe contener al menos 32 bytes UTF-8. Si queda vacío en local, se genera una clave aleatoria por arranque: reiniciar invalida los tokens previos. Para producción activa `SPRING_PROFILES_ACTIVE=prod` y configura una clave propia; este perfil rechaza claves ausentes/cortas y fija BCrypt en costo 12.

`.env.example` conserva además las variables S3/OpenAI para contratos futuros. Estos servicios aún no están integrados ni validados. Las credenciales AWS se obtendrán del proveedor estándar, nunca de Angular. No publiques `.env` ni claves.

## 3. Arranque del backend

```powershell
cd backend
mvn spring-boot:run
```

El backend escucha en `http://localhost:8080`. Si `java` no apunta al JDK correcto, configura `JAVA_HOME` a tu JDK 17 y añade su carpeta `bin` a PATH. En este equipo se detectó `C:\Program Files\Java\jdk-17`; el acceso directo Oracle de PATH no respondió correctamente durante la inspección.

Maven se descargó localmente para las verificaciones. Si todavía no está en PATH, desde `backend/` puedes usar `..\.tools\apache-maven-3.9.9\bin\mvn.cmd` en lugar de `mvn`, con `JAVA_HOME` configurado. `.tools/` no se versiona.

Prueba la API en un segundo terminal:

```powershell
$puestos = Invoke-RestMethod http://localhost:8080/api/v1/puestos
$puestos | Format-Table id_puesto, nombre_puesto
$idPuesto = $puestos[0].id_puesto
Invoke-RestMethod "http://localhost:8080/api/v1/puestos/$idPuesto/preguntas"
```

Catálogo, registro y login son públicos. El perfil requiere Bearer JWT y cuenta ACTIVO/POSTULANTE. Consulta los contratos de [catálogo](docs/api-catalogo.md) e [identidad](docs/api-identidad.md).

## 4. Arranque del frontend

En otro terminal desde la raíz:

```powershell
cd frontend
npm ci
npm start
```

Abre [http://localhost:4200](http://localhost:4200). `proxy.conf.json` reenvía `/api/**` a `127.0.0.1:8080`. El backend debe estar iniciado para obtener datos. Puedes seleccionar cualquier puesto y leer sus tres preguntas; no hay datos de respaldo simulados. Si el servidor no responde, se muestra un error con reintento.

Recorrido del Incremento 2: `/registro` → `/login` → `/panel` → `/perfil`. Las contraseñas requieren 12 puntos de código Unicode como mínimo y 72 bytes UTF-8 como máximo; sus espacios se conservan. Confirmación debe coincidir exactamente. Tras registrarte inicia sesión con esas credenciales. En el perfil solo se editan nombre y resumen; el correo es de solo lectura.

Token y usuario se mantienen únicamente en memoria: cerrar sesión, vencer 30 minutos o recargar la aplicación requiere volver a acceder. El cierre local no revoca una copia del JWT antes de su expiración. La cabecera cambia según el estado de autenticación y las rutas privadas están protegidas tanto en Angular como en el backend.

## 5. Pruebas y compilación

```powershell
cd backend
mvn test
mvn package
cd ..\frontend
npm run build
```

`mvn test` utiliza H2 y las mismas migraciones Flyway, sin AWS/OpenAI. Comprueba dominio, capas, JWT, BCrypt, registro, perfil y catálogo. Las dos pruebas específicas de MySQL se omiten en este modo. Integración usa BCrypt de costo 4; otra prueba comprueba costo 12. H2 no certifica todos los tipos de MySQL. Si conservas `TEST_DB_URL` de una ejecución anterior, elimina esa variable antes del pase H2 (`Remove-Item Env:TEST_DB_URL`).

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
- [Verificación del Incremento 1](docs/verificacion_incremento_1.md).
- [Verificación del Incremento 2](docs/verificacion_incremento_2.md).

El Incremento 1 está guardado en `c626aaf` con el mensaje aprobado. GitHub CLI no se encuentra instalado/disponible en este equipo; no se publicó ni configuró `origin`. El Incremento 2 queda en el árbol de trabajo, sin commit, para la auditoría. La documentación de diseño original se mantiene fuera del repositorio.
