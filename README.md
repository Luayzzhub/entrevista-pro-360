# Entrevista Pro 360 — Incrementos 1 a 5

Base del MVP universitario: Spring Boot de cuatro capas, MySQL/Flyway y Angular VIS-1.0. Incluye identidad, catálogo, sesiones, captura/revisión local y confirmación idempotente de video: ffprobe valida el contenido y S3 recibe una escritura privada condicional. La respuesta queda SUBIDO con contexto PROC-1.0; MCK 08 permite consultar su estado y recuperarlo desde la sesión. Extracción/transcripción/métricas corresponden al Incremento 6 y evaluación al 7.

## Requisitos

| Herramienta | Requisito del proyecto |
| --- | --- |
| Java JDK | 17 (compilación objetivo); JDK 21 también puede compilar para 17 |
| Maven | 3.6.3 o superior; verificación realizada con 3.9.9 |
| Node.js | 20.19+, 22.12+ o 24.x, según compatibilidad de Angular 21 |
| npm | Incluido con Node; usar `npm ci` con el lock entregado |
| Angular CLI | 21.2.0 local, instalado mediante `npm ci`; no requiere instalación global |
| MySQL | 8.0+ con una base exclusiva del proyecto y usuario con permisos para ejecutar migraciones |
| Git | Incrementos 1 a 4 publicados; Incremento 5 auditado y aprobado sin observaciones, confirmado en este commit de main y publicado al cerrar el incremento |
| Navegador y dispositivos | Cámara + micrófono; `getUserMedia` y MediaRecorder con uno de los tres formatos negociados. Usar localhost/loopback o HTTPS |
| ffprobe / ffmpeg | 9.0.2 portable verificado. ffprobe inspecciona los uploads; ffmpeg genera fixtures de pruebas. Extracción de audio automática pendiente |

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

S3 está integrado desde Incremento 5. Configura AWS_REGION y S3_BUCKET con un bucket privado y bloqueo de acceso público; el servidor necesita PutObject, GetObject (HEAD) y DeleteObject para sus claves. El SDK obtiene credenciales del proveedor estándar de AWS (rol, perfil o variables AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY), nunca de Angular. S3_ENDPOINT_OVERRIDE queda vacío para AWS y admite un endpoint S3 de pruebas local con acceso por ruta. La verificación entregada usa HTTP local con SDK real; no se aprovisionó ni se certificó un bucket de producción en AWS. OpenAI permanece pendiente. No publiques .env ni claves.

FFPROBE_BIN indica el ejecutable (por defecto ffprobe). En Windows el adaptador encuentra automáticamente .tools/ffmpeg/ffprobe.exe desde la raíz o backend. Si no dispones de binarios, descarga una compilación Windows enlazada desde [FFmpeg](https://ffmpeg.org/download.html) y coloca ffprobe.exe y ffmpeg.exe en esa carpeta ignorada; también puedes usar PATH o una ruta explícita en FFPROBE_BIN. Las pruebas necesitan ffmpeg real para generar sus archivos; FFMPEG_BIN permite seleccionar su ruta. No hay simulación de inspección ni descarga automática al atender una petición.

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

Catálogo, registro y login son públicos. Perfil y sesiones requieren Bearer JWT y cuenta ACTIVO/POSTULANTE; las sesiones se consultan/cancelan exclusivamente por su propietario. Consulta los contratos de [catálogo](docs/api-catalogo.md), [identidad](docs/api-identidad.md) y [sesiones](docs/api-sesiones.md) y [respuestas](docs/api-respuestas.md).

## 4. Arranque del frontend

En otro terminal desde la raíz:

```powershell
cd frontend
npm ci
npm start
```

Abre [http://localhost:4200](http://localhost:4200). `proxy.conf.json` reenvía `/api/**` a `127.0.0.1:8080`. El backend debe estar iniciado para obtener datos. Puedes seleccionar cualquier puesto y leer sus tres preguntas; no hay datos de respaldo simulados. Si el servidor no responde, se muestra un error con reintento.

Recorrido del Incremento 2: `/registro` → `/login` → `/panel` → `/perfil`. Las contraseñas requieren 12 puntos de código Unicode como mínimo y 72 bytes UTF-8 como máximo; sus espacios se conservan. Confirmación debe coincidir exactamente. Tras registrarte inicia sesión con esas credenciales. En el perfil solo se editan nombre y resumen; el correo es de solo lectura.

Recorrido del Incremento 3: `/panel` → **Nueva práctica** → `/puestos` → **Ver preguntas** o **Iniciar práctica** → `/sesiones/:id`. La pregunta 1 queda disponible; la siguiente solo se habilita cuando la anterior tiene una respuesta persistida en `EVALUADO`. Este incremento no graba ni sube videos. Mientras exista una sesión pendiente, panel/catálogo/preguntas permiten continuarla o finalizarla con confirmación explícita. Cancelar conserva las respuestas y no genera promedio; después se puede crear otra práctica. Dos POST concurrentes para el mismo usuario producen un 201 y un 409 bajo bloqueo pesimista de su fila en MySQL.

Recorrido del Incremento 4 dentro de la pregunta habilitada:

1. Lee el aviso de procesamiento futuro y pulsa **Comprobar cámara y micrófono**. Autoriza ambos dispositivos; se comprueban pistas vivas y se muestra una vista previa silenciada. Si deniegas el acceso, cambia los permisos en el navegador y pulsa **Volver a intentar permisos**.
2. Pulsa **Iniciar grabación**. Mantén el contexto de puesto, pregunta, competencia y guía STAR cuando corresponda. El contador usa un reloj monotónico y detiene automáticamente al borde de 60 s; puedes pulsar **Detener grabación** antes.
3. Reproduce la toma con imagen/audio y revisa duración, formato y bytes. Se exige `1 <= duración <= 60` y `0 < bytes < 50_000_000`. **Volver a grabar** descarta y revoca la toma local y vuelve a preparar la misma pregunta.

En Incremento 5 **Confirmar respuesta se habilita para una toma local válida**. Mientras sube se bloquean confirmar/repetir, se muestra progreso y se pide mantener abierta la pestaña. Un fallo de red o S3 conserva la toma para volver a confirmar; 413/415/422 permiten repetirla. Solo un 202 revoca la toma y navega a /sesiones/:idSesion/respuestas/:idRespuesta (MCK 08).

Grabar/repetir no crea filas; confirmar sí persiste una respuesta SUBIDO y cambia la sesión a EN_PROCESO. MCK 08 muestra los metadatos medidos por ffprobe y consulta el estado cada 3 s; volver al panel/sesión permite reabrirlo. No se habilita la pregunta siguiente hasta EVALUADO. El trabajador de procesamiento todavía no se ejecuta, por lo que este incremento mantiene SUBIDO.

La negociación prueba, en orden, WebM VP8/Opus, WebM VP9/Opus y MP4 H.264/AAC; ninguna opción disponible bloquea la captura. La duración local mide el intervalo solicitado; el servidor inspecciona las pistas/códecs y la duración real con ffprobe. Acepta WebM streaming sin duración en cabecera leyendo los timestamps de sus paquetes. Exige duración 1..60 s antes de redondear y tamaño estrictamente menor a 50 000 000 bytes.

Token y usuario se mantienen únicamente en memoria: cerrar sesión, vencer 30 minutos o recargar la aplicación requiere volver a acceder. El cierre local no revoca una copia del JWT antes de su expiración. La cabecera cambia según el estado de autenticación y las rutas privadas están protegidas tanto en Angular como en el backend.

## 5. Pruebas y compilación

```powershell
cd backend
mvn test
mvn package
cd ..\frontend
npm run test:media
npm run build
```

`mvn test` utiliza H2 y las mismas migraciones Flyway, sin AWS/OpenAI. Comprueba dominio, capas, JWT, BCrypt, registro, perfil, catálogo y sesiones, incluida concurrencia HTTP real. Las dos pruebas específicas de MySQL se omiten en este modo. Integración usa BCrypt de costo 4; otra prueba comprueba costo 12. H2 no certifica todos los tipos de MySQL. Si conservas `TEST_DB_URL` de una ejecución anterior, elimina esa variable antes del pase H2 (`Remove-Item Env:TEST_DB_URL`).

La suite de confirmación requiere ffprobe y ffmpeg reales. Genera fixtures propios y usa S3Client real contra HttpServer local efímero, sin necesitar cuenta AWS ni levantar un S3 externo para mvn test. Las pruebas de confirmación limpian sus filas; no uses una base personal.

Para verificar contra MySQL, crea **otra base exclusiva de pruebas**, con el mismo charset y usuario autorizado. Las pruebas de catálogo/identidad usan transacciones revertidas. Las de confirmación y concurrencia usan transacciones reales y eliminan sus registros propios al terminar; la prueba de compensación crea y elimina un trigger de fallo exclusivamente en la base de pruebas. La base debe contener únicamente la semilla inicial. Desde `backend/`:

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
- [Contrato de sesiones](docs/api-sesiones.md).
- [Verificación del Incremento 3](docs/verificacion_incremento_3.md).
- [Verificación del Incremento 4](docs/verificacion_incremento_4.md).
- [Contrato de respuestas](docs/api-respuestas.md).
- [Verificación del Incremento 5](docs/verificacion_incremento_5.md).

Los Incrementos 1 a 4 están publicados en [Luayzzhub/entrevista-pro-360](https://github.com/Luayzzhub/entrevista-pro-360). El Incremento 5 fue auditado y aprobado sin observaciones; se cierra en este commit atómico de main con el mensaje aprobado y se publica mediante git push origin main. El Incremento 6 corresponde al worker durable PROC-1.0, extracción WAV con ffmpeg, transcripción gpt-4o-mini-transcribe-2025-12-15, cálculo determinista MET-1.0 y reintento manual. Los informes previos conservan su alcance histórico. La documentación de diseño original permanece fuera del repositorio.
