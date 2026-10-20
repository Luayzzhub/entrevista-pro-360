# Verificación del Incremento 2

Fecha: 20 de octubre de 2026, America/Lima. Alcance: RF-01 registro, RF-02 acceso/salida y RF-03 perfil básico. Código completo disponible en el árbol de trabajo, **sin commit del Incremento 2**, para revisión del orquestador.

## Paso 1: cierre del Incremento 1

- Eliminada la referencia retirada de la línea 3 de arquitectura antes del primer commit.
- `.gitignore` comprobado: excluye `.tools/`, `node_modules/`, `target/`, `dist/`, `.angular/`, `.env`, credenciales, medios y binarios/temporales.
- Primer commit creado en `main`: **`c626aaf`**, con 73 archivos.
- Mensaje exacto: `feat: inicializar arquitectura de 4 capas, esquema MySQL de 8 tablas y catalogo`.
- `gh` no se encuentra en PATH ni en la ubicación estándar de GitHub CLI; se aplicó la alternativa autorizada de conservar el commit local y continuar el desarrollo.
- No se creó repositorio GitHub, no hay `origin` ni push. Publicación pendiente de disponer de GitHub CLI autenticado.

## Resultados ejecutados

| Comprobación | Resultado |
| --- | --- |
| `mvn test` (H2) | **BUILD SUCCESS**: 44 casos registrados, 42 ejecutados, 0 fallos, 0 errores y 2 omitidos exclusivos de MySQL |
| `mvn test -Dspring.profiles.active=mysql` | **BUILD SUCCESS**: 44 ejecutados, 0 fallos, 0 errores y 0 omitidos |
| `LayerDependenciesTest` | Correcto: dominio/aplicación independientes de Spring, JPA y clientes externos |
| `mvn package -DskipTests` posterior a las pruebas | **BUILD SUCCESS**, JAR ejecutable generado |
| `npm run build` final | Correcto: producción AOT/plantillas estrictas; carga inicial 263,43 kB y transferencia estimada 72,05 kB, dentro del presupuesto |
| Lock npm | `@angular/forms:21.2.0` instalado; todas las dependencias directas coinciden con manifiesto y lock |
| Esquema MySQL | Se conservan V1/V2, ocho tablas y 68 columnas; no hay cambios en migraciones aprobadas |
| JAR y navegador reales | Registro/login/perfil conectados mediante Angular → proxy → JAR → MySQL, sin respuestas simuladas |
| Estado Git final | HEAD permanece en `c626aaf`; Incremento 2 sin commit y sin archivos preparados en el índice |

Entorno: JDK 17.0.12, Maven 3.9.9, Spring Boot 3.5.7, MySQL 8.0.44, Node 24.19.0, npm 11.17.0 y Angular 21.2.0. La instancia MySQL temporal utilizó la base exclusiva `ep360_incremento2_test`, puerto 33307 y datos dentro de `.tools/`. Backend temporal en 18080 y Angular en 14200. Los puertos normales siguen siendo 8080 y 4200.

## Cobertura automatizada

| Suite | Casos | Comprobaciones |
| --- | --- | --- |
| `UsuarioTest` | 11 | Trim de nombre, correo Locale.ROOT, formato/límites, resumen null/blanco/500, texto plano, 11/12 puntos de código, 72 bytes/multibyte/emojis, espacios preservados, asignación en servidor e identidad inmutable al editar |
| `AuthPerfilIntegrationTest` | 13 | Registro 201 y ocho propiedades públicas; BCrypt real; duplicado 409 y colisión SQL real; validación/mass assignment 400; emojis; JWT HS256/sub/rol/iat/exp/1800; 401 genérico; GET/PATCH del propietario; resumen null; campos prohibidos; tokens vencidos/manipulados; cuentas desactivadas, bloqueadas, con rol cambiado o eliminadas |
| `IdentidadConfigTest` | 3 | Clave mínima en bytes UTF-8, rechazo de clave corta/ausente en producción, generación aleatoria local y BCrypt costo 12 |
| `LayerDependenciesTest` | 1 | Restricción de imports de capas internas |
| `PuestoPreguntaTest` | 6 | Invariantes del catálogo del Incremento 1 |
| `CatalogoIntegrationTest` | 8 | Catálogo, orden, STAR, errores y seguridad, sin regresiones |
| `MySqlSchemaIntegrationTest` | 2 | Tablas/columnas/motor/collation/JSON/claves foráneas/Flyway; solo se ejecuta con MySQL |

Los endpoints se prueban con seguridad, casos de uso, BCrypt, JOSE, Flyway y JPA reales. Las pruebas usan costo BCrypt 4 para rapidez; costo 12 tiene prueba independiente y fue el utilizado por el JAR de la revisión manual.

## Recorrido observado en Angular

Se utilizó una cuenta sintética de revisión, sin datos personales reales.

1. Registro muestra los cuatro campos, aviso de privacidad y validación de datos.
2. Once emojis y una confirmación diferente muestran errores en cliente y no crean la cuenta.
3. Registro válido con espacios exteriores en nombre/password crea la cuenta y lleva a login con confirmación de éxito.
4. Intentar login omitiendo los espacios del password original se rechaza con el mensaje genérico; el texto exacto permite entrar.
5. Panel muestra bienvenida y cuenta, navegación autenticada, Nueva práctica y Mi perfil. No presenta sesiones o estadísticas ficticias.
6. Perfil obtiene datos reales, mantiene correo de solo lectura y guarda nombre/resumen; la cabecera refleja el nombre normalizado y aparece aviso de éxito.
7. Un resumen con etiquetas se conserva como texto plano en el textarea; el contador refleja sus caracteres. Un resumen blanco se normaliza a null y vuelve a mostrarse vacío.
8. Retroceder a login con sesión vigente redirige al panel (`publicOnlyGuard`).
9. Recargar el panel elimina el estado en memoria y redirige a login (`authGuard`).
10. Volver a acceder y pulsar Cerrar sesión limpia la cuenta y devuelve login/cabecera pública; retroceder al panel permanece protegido.

Se revisó visualmente el panel en el navegador, con colores y jerarquía VIS-1.0, sin superposiciones. No se afirma una matriz completa de dispositivos ni una certificación integral de accesibilidad.

## Decisiones de seguridad y límites

- HS256 con clave de al menos 32 bytes UTF-8. Sin variable en local, clave criptográficamente aleatoria por arranque; con perfil `prod`, secreto obligatorio. No hay valor fijo compartido de producción.
- Expiración exacta `exp - iat = 1800`. El servidor rechaza tokens vencidos sin tolerancia adicional y consulta existencia/estado/rol de la cuenta en cada petición privada.
- El cliente programa su limpieza con `expires_in`, también comprueba vencimiento en guard/interceptor y no usa localStorage/sessionStorage. No se esperaron 1800 segundos reales durante la revisión del navegador; la expiración del servidor se prueba con JWT ya vencidos y el temporizador cliente se revisó en el código.
- Un 401 privado limpia/redirige; un 401 de login se muestra junto al formulario. Respuestas tardías anteriores no restauran el usuario ni eliminan una sesión nueva.
- Cerrar sesión es una eliminación local: una copia previa del token conserva su vigencia máxima, sujeta al estado actual de la cuenta. No hay refresh ni revocación inmediata ni promesa de bloqueo tras cinco fallos.
- No se añadieron tablas/columnas ni se cambiaron los contratos de catálogo. Sesiones, captura, S3, STT, evaluación, resultados e historial siguen pendientes para incrementos posteriores.

## Evidencias y revisión

[`evidencias/pruebas-incremento-2.json`](evidencias/pruebas-incremento-2.json) contiene suites/casos extraídos de Surefire, conteos H2/MySQL, versiones directas de frontend, resultado de build y separación entre commits. Los logs completos permanecen ignorados en `.tools/inc2-*.log`; los reportes XML originales, en `backend/target/surefire-reports/`. No se guardan tokens, contraseñas ni hashes en las evidencias versionables.

Archivos principales nuevos: servicios/puertos de identidad, invariantes `Usuario`, adaptador y entidad/repositorio JPA de usuario, BCrypt/JWT, controladores/DTO de auth/perfil, configuración prod y tres suites nuevas. En Angular: AuthService, interceptor/guards/modelos, validadores, login/registro/perfil/panel y cabeceras. Se actualizaron README, `.env.example`, arquitectura, dependencias, matriz RF y contrato de catálogo; se agregó `api-identidad.md`.

El commit del Incremento 1 queda separado. El Incremento 2 se entrega para auditoría con modificaciones y archivos nuevos en el árbol de trabajo.
