# Arquitectura — Incrementos 1 a 3

Backend modular Spring Boot 3.5.7, Java 17, paquete `pe.utp.entrevistapro360`; frontend Angular 21.2.0 con VIS-1.0. Se implementan catálogo, identidad y gestión de sesiones de entrevista.

## Cuatro capas

| Capa | Responsabilidad | Componentes reales |
| --- | --- | --- |
| `presentation` | HTTP, DTO públicos, estructura de solicitudes y errores JSON | `CatalogoController`, `AuthController`, `PerfilController`, `SesionController`, DTO, `GlobalExceptionHandler`, `SecurityExceptionHandler` |
| `application` | Casos de uso y puertos independientes de frameworks | Servicios de catálogo, registro, autenticación, perfil y sesiones; puertos de entrada/salida y modelos puros de lectura |
| `domain` | Modelos e invariantes puros | `Puesto`, `Pregunta`, `Usuario`, excepciones de validación y otros cinco modelos canónicos |
| `infrastructure` | JPA, BCrypt, JWT, seguridad y composición | Adaptadores/repositorios JPA, `BCryptPasswordHasher`, `JwtTokenProvider`, `JwtAuthenticationFilter`, configuraciones |

```mermaid
flowchart LR
  UI[Angular / servicios HTTP] --> REST[presentation / controladores]
  REST --> UC[application / casos de uso y puertos]
  UC --> D[domain / invariantes]
  JPA[infrastructure / adaptadores JPA] -. implementa .-> UC
  CRYPTO[infrastructure / BCrypt y JOSE] -. implementa .-> UC
  JPA --> DB[(MySQL / Flyway)]
```

Dominio y aplicación solo importan Java y las capas internas permitidas. `LayerDependenciesTest` mantiene esta regla. Se inyecta `java.time.Clock` para obtener UTC sin depender de Spring en los casos de uso. Los controladores usan puertos de entrada y nunca acceden a JPA. La infraestructura implementa puertos y compone servicios; la serialización de errores permanece en presentación.

## Catálogo y persistencia

Se conservan los GET públicos del Incremento 1. El caso de uso busca un puesto activo y valida exactamente tres preguntas activas del mismo puesto, con órdenes 1, 2 y 3. Ausencia/inactividad produce 404; catálogo incompleto, 409. La semilla resuelve IDs por nombre único.

No se modifican V1/V2: ocho tablas y 68 columnas de negocio, InnoDB, `utf8mb4_unicode_ci`, JSON y acciones referenciales. `flyway_schema_history` es una tabla técnica adicional. Hibernate usa `ddl-auto=validate`; Flyway controla el esquema. Están activos cinco mapeos JPA: usuarios, puestos, preguntas, sesiones y respuestas de video. La respuesta de video se consulta, sin endpoint de carga todavía. Sus diez columnas incluyen `procesamiento_contexto` como JSON mediante `@JdbcTypeCode(SqlTypes.JSON)`.

Las operaciones JPA son transaccionales en los adaptadores. Las pruebas MVC atraviesan seguridad, casos de uso y JPA reales, sin mocks del catálogo o identidad. H2 usa las mismas migraciones en modo MySQL; el perfil de pruebas `mysql` repite la integración y comprueba metadatos reales.

## Registro y acceso

`RegistrarUsuarioService` normaliza nombre/correo, valida contraseña, comprueba correo existente, calcula BCrypt y persiste. El dominio asigna POSTULANTE/ACTIVO, fecha UTC, actualización nula y resumen nulo. La restricción `uk_usuarios_email` resuelve también colisiones concurrentes: el adaptador la traduce a `EmailYaRegistradoException`, que produce 409. Se prueban el chequeo previo y una colisión SQL real.

`AutenticarUsuarioService` valida datos y compara el password exacto con BCrypt. Un usuario desconocido también realiza una comparación con hash ficticio. Contraseña incorrecta, usuario desconocido, cuenta inactiva o rol incompatible producen el mismo mensaje 401. No se promete bloqueo tras cinco intentos.

BCrypt usa costo 12 por defecto y en el perfil `prod`; las pruebas de integración usan costo 4. Una prueba separada verifica costo 12 y espacios. La contraseña nunca se recorta ni se normaliza silenciosamente. Se exigen al menos 12 puntos de código Unicode y como máximo 72 bytes UTF-8.

## JWT y autenticación por petición

`JwtTokenProvider` usa `NimbusJwtEncoder` con HS256. Incluye `iss=entrevista-pro-360`, `sub=id_usuario`, `rol=POSTULANTE`, `iat` y `exp=iat+1800`. `NimbusJwtDecoder` verifica firma/algoritmo; validadores comprueban issuer, timestamps, duración exacta y vencimiento sin tolerancia adicional.

En rutas privadas, `JwtAuthenticationFilter` toma el ID verificado y consulta la base en cada petición. Solo autentica si la cuenta existe, está ACTIVO y conserva rol POSTULANTE. Ausencia de token, firma/tiempo inválidos, cuenta eliminada/inactiva/bloqueada o rol incompatible producen 401 con `ApiError`. El contexto de seguridad contiene el ID; el perfil no usa IDs enviados por el cliente para seleccionar al propietario.

`JWT_SECRET` requiere al menos 32 bytes UTF-8. Si está vacío en local, se generan 32 bytes criptográficamente aleatorios por arranque, compartidos por encoder/decoder y nunca impresos. Reiniciar invalida esos tokens. El perfil `prod` impide la clave efímera y exige una variable válida. El fixture conocido de pruebas solo aparece en recursos de test.

La API es stateless, sin formulario de Spring, Basic ni usuario automático. CSRF está deshabilitado para la credencial explícita en Authorization, sin cookies de autenticación. Catálogo y `/auth/**` son públicos; el resto exige autenticación.

## Perfil y DTO

`GestionarPerfilService` consulta al propietario autenticado y construye un nuevo `Usuario` cambiando solo nombre, resumen y fecha de actualización. El adaptador modifica únicamente esas columnas. ID, correo, hash, rol, estado y fecha de registro se conservan.

Nombre: trim, 1..150. Correo: trim, minúsculas Locale.ROOT, formato válido y máximo 150. Resumen: trim, vacío a null, máximo 500 y texto plano. Jackson usa `SNAKE_CASE`, rechaza propiedades desconocidas y coerción de escalares. PATCH rechaza correo, rol, estado, hash, IDs y cualquier propiedad ajena al DTO. Las respuestas enumeran ocho propiedades públicas y nunca exponen el hash. Las fechas usan ISO 8601 con `Z` y DATETIME UTC con precisión de segundos.

Los errores mantienen `codigo`, `mensaje` y `errores_campo`. Los contratos HTTP se detallan en `api-catalogo.md`, `api-identidad.md` y `api-sesiones.md`. Los nombres de campos de Bean Validation también se convierten a snake_case, incluido `id_puesto`; se rechaza coerción de decimales a IDs enteros.

## Sesiones: unidad transaccional y bloqueos

`GestionarSesionService` implementa el puerto de entrada puro `GestionarSesion`; `SesionRepository` declara consultas y límites transaccionales con callbacks Java (`Supplier`/`Function`). La aplicación no importa anotaciones de Spring. `SesionJpaAdapter` ejecuta el callback dentro de una transacción Spring: el bloqueo vive hasta que termina toda la operación y se confirma el commit, sin liberarse entre comprobación e inserción.

Para crear, `conUsuarioBloqueado` usa READ_COMMITTED y su primera lectura es `UsuarioJpaRepository.findByIdForUpdate` con PESSIMISTIC_WRITE. Bajo esa exclusión consulta sesión INICIADA/EN_PROCESO, valida catálogo, crea con `LocalDateTime.now(clock).withNano(0)` y ejecuta `saveAndFlush`. Dos solicitudes para un mismo usuario se serializan; la segunda observa la sesión confirmada por la primera y recibe 409 con el ID vigente. No se usa un candado en memoria ni se añade una tabla/columna/índice.

Para cancelar, `findPropiaForUpdate` bloquea la sesión filtrando simultáneamente ID y propietario. El dominio cancela INICIADA/EN_PROCESO, conserva fechas si ya estaba CANCELADA y rechaza COMPLETADA. El promedio de CANCELADA es siempre null. Se conservan respuestas. Sesiones inexistentes y ajenas generan el mismo 404.

`VistaSesion`, `EstadoPreguntaSesion`, `ResumenSesionItem` y `ResumenSesionesUsuario` son modelos puros de lectura. El avance se calcula a partir de las respuestas persistidas: una etapa distinta de EVALUADO bloquea la siguiente pregunta. La consulta de resumen carga las respuestas por colección de sesiones y reutiliza catálogos por puesto; ordena items por inicio/ID y selecciona la última completada por fecha final/ID. El frontend presenta el promedio almacenado sin volver a calcularlo.

Una prueba usa dos POST HTTP concurrentes al servidor embebido en puerto aleatorio, con una cuenta confirmada y dos transacciones independientes, tanto en H2 como MySQL. Las pruebas de datos escritos por JDBC y JPA comparten la misma configuración UTC que producción; se comprueba también el DATETIME almacenado. `hibernate.type.java_time_use_direct_jdbc=true` enlaza `LocalDateTime` mediante JDBC 4.2, conservando sus componentes UTC sin la conversión intermedia de `java.sql.Timestamp` dependiente de la zona del JVM. `hibernate.jdbc.time_zone=UTC` se conserva para los demás tipos temporales. Esta configuración se verifica con JVM en America/Lima y base/API en UTC.

## Angular

Componentes standalone y rutas diferidas por funcionalidad. `AuthService` usa signals y mantiene token/usuario únicamente en memoria. Con `expires_in` programa limpieza y navegación a `/login`; guards e interceptor comprueban también vencimiento al actuar. Cerrar sesión elimina ambos datos y el temporizador. Recargar crea un estado no autenticado.

El interceptor agrega Bearer solo a rutas privadas de la API propia y limpia/redirige ante 401 privado. El 401 de login se muestra en su formulario. Una respuesta tardía anterior no restaura el usuario ni elimina una sesión posterior. Los guards protegen panel/perfil/sesiones; login/registro redirigen al panel si existe sesión vigente.

La cabecera pública ofrece Puestos/Iniciar sesión/Crear cuenta; la autenticada, Panel/Puestos/Perfil/nombre/Cerrar sesión. Reactive Forms implementa validadores Unicode/UTF-8, confirmación exacta, errores de campo y estados de envío. El perfil tiene correo readonly, contador de resumen y avisos. El resumen se presenta como valor de formulario, sin `innerHTML`. VIS-1.0 conserva Arial, colores, foco y adaptación móvil.

Cerrar sesión no revoca un JWT copiado previamente: vence a los 30 minutos y el backend sigue comprobando la cuenta. No hay renovación automática ni tabla de sesiones de autenticación.

`SesionService` implementa las cuatro consultas/mutaciones tipadas. El panel consulta su resumen real; `ContextoPractica`, con alcance por vista, comparte una consulta de sesión pendiente entre aviso y botones de catálogo/preguntas. Se cancelan suscripciones al destruir la vista. Un 409 de creación permite ir a la sesión indicada por la API. Toda cancelación requiere dos acciones explícitas en el componente compartido `CancelarPracticaComponent`.

`SesionComponent` valida el ID antes de consultar, presenta metadatos/progreso/fechas UTC, la pregunta habilitada y el estado individual de las tres preguntas. STAR se muestra solo cuando corresponde. Los estados terminales no ofrecen captura ni cancelación. No hay datos de respaldo simulados ni promedios inventados para sesiones sin completar.

## Alcance siguiente

El panel permite iniciar, continuar y cancelar sesiones y editar el perfil. La captura de cámara/micrófono/video (RF-07..RF-10) se activa en el Incremento 4. Multimedia, S3, STT, evaluación, resultados, cierre automático e historial con filtros continúan pendientes. JOSE tiene uso real; SDK S3 sigue reservado. No se presenta la disponibilidad de modelos externos como validada.
