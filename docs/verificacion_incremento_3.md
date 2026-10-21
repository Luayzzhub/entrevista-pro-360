# Verificación del Incremento 3

Fecha de verificación: 20/10/2026, America/Lima. Base de pruebas: rama `main`, commit `e47eeb6affc14b00fbcfc81912e304d145d47819`. Cierre 21/10/2026: el Orquestador auditó y aprobó el Incremento 3; se confirma en este commit de `main`.

## Alcance comprobado

RF-04/RF-05/RF-06: catálogo/preguntas conectados a creación de práctica, sesión única no terminal por usuario, detalle y resumen propios, avance secuencial desde respuestas persistidas, cancelación anticipada idempotente y vistas MCK 03/MCK 04/sesión. No hay respuestas, evaluaciones ni promedios simulados en la aplicación.

Las migraciones V1/V2 permanecen idénticas al commit base; no se añadió ninguna migración, tabla ni columna. MySQL acredita **8 tablas de negocio y 68 columnas**, con `ddl-auto=validate`. `flyway_schema_history` es técnica y adicional. Los SHA-256 y resultados por suite/caso se conservan en [pruebas-incremento-3.json](evidencias/pruebas-incremento-3.json).

## Resultados finales

| Verificación | Resultado |
| --- | --- |
| `mvn test` — H2 2.3.232 | 73 casos descubiertos; **71 ejecutados**, 2 omitidos exclusivos de MySQL; 0 fallos, 0 errores |
| `mvn test -Dspring.profiles.active=mysql` — MySQL 8.0.44 real | **73 ejecutados**, 0 omitidos, 0 fallos, 0 errores |
| Nuevos casos de sesiones | 15 de dominio + 14 de integración; los 44 anteriores se conservan |
| `LayerDependenciesTest` | Correcto; dominio/aplicación solo importan Java y capas internas permitidas |
| Concurrencia HTTP real en ambos motores | Dos POST simultáneos al servidor embebido, dos transacciones: exactamente **201 + 409**, una sola sesión no terminal y el mismo ID en el conflicto |
| `mvn package -DskipTests` | BUILD SUCCESS; JAR ejecutable generado y arrancado contra MySQL |
| `npm run build` | Compilación Angular de producción correcta, **sin advertencias de presupuesto**; inicial 284.21 kB, transferencia estimada 77.85 kB |
| Navegador | Chrome headless mediante Playwright, JAR + MySQL + Angular; 7 grupos de comprobaciones, 24 peticiones de sesiones, 0 errores JavaScript |
| Adaptación de sesión | Escritorio y 390 px; sin desborde horizontal y capturas inspeccionadas |

Se usó un MySQL aislado en `127.0.0.1:33307`, base exclusiva `ep360_incremento3_test`. La preview utilizó JAR en 18080 y Angular en 14200. No se usaron bases personales ni proveedores AWS/OpenAI. Los comandos reproducibles generales están en README; `.tools/` contiene auxiliares/logs locales ignorados por Git.

## Cobertura de dominio e integración

`SesionEntrevistaTest` verifica identidades y fechas, catálogo de tres preguntas activas 1/2/3, estados terminales/no terminales, promedio obligatorio solo en COMPLETADA, rango/escala/HALF_UP, avance EVALUADO, bloqueo en SUBIDO/AUDIO_EXTRAIDO/TRANSCRITO/FALLIDO, contadores propios y cancelación desde ambos estados no terminales. La cancelación repetida conserva el objeto/fecha; un reloj anterior al inicio no genera fecha final inválida.

`SesionIntegrationTest` atraviesa seguridad, controladores, aplicación y JPA reales. Comprueba 201/Location, DTO cerrado y snake_case, 400 por campos/IDs inválidos, 401 en todas las rutas privadas, 404 de puesto inactivo/inexistente, 409 por catálogo incompleto/sesión pendiente/completada, propiedad desde principal, consulta y cancelación ajenas indistinguibles de inexistentes, progreso por respuestas persistidas, preservación de respuestas al cancelar, idempotencia, nueva práctica tras cancelar y resumen exclusivo ordenado por inicio/ID con última completada por fin/ID.

La prueba concurrente usa `TestRestTemplate`, servidor HTTP en puerto aleatorio, `ExecutorService` y barrera de salida. Desactiva la transacción envolvente del test; las solicitudes usan transacciones independientes y la cuenta temporal queda confirmada antes del envío. En `finally` elimina únicamente sus datos. El resto de fixtures se revierte por transacción. No se usan mocks de repositorios ni endpoints especiales de prueba.

## Corrección de persistencia temporal

La verificación del DATETIME almacenado detectó una conversión intermedia de `java.sql.Timestamp` que desplazaba cinco horas los valores UTC con JVM en America/Lima. Se habilitó `hibernate.type.java_time_use_direct_jdbc=true` en producción y pruebas, conservando `hibernate.jdbc.time_zone=UTC`: `LocalDateTime` pasa directamente por JDBC 4.2 y conserva los componentes UTC. La prueba compara la fecha obtenida por JDBC con el instante UTC de la respuesta API; las suites finales de H2 y MySQL pasan con esta configuración. No se modificó el esquema ni se ocultó el problema cambiando la zona del JVM.

## Recorrido comprobado en navegador

1. Exploración pública de tres puestos y tres preguntas, con enlace para acceder y sin botones que creen sesiones anónimas.
2. Registro/acceso mediante formularios y API reales. MCK 03 muestra el texto de feedback acordado, cero sesiones completadas y ningún promedio inventado.
3. MCK 03 → Nueva práctica → MCK 04 → Ver preguntas → Iniciar práctica. POST 201 y navegación a `/sesiones/43`, con pregunta 1 disponible, 2/3 bloqueadas, progreso 0/3, 60 segundos y aviso honesto de captura futura; la primera pregunta indica que no requiere STAR.
4. Regreso al panel, catálogo y preguntas: todos muestran la misma práctica pendiente y permiten continuarla. Los botones de nueva práctica permanecen deshabilitados en catálogo/preguntas.
5. En sesión, abrir la confirmación y pulsar **Seguir practicando** no envía cancelación. Confirmar después produce POST 200 y estado CANCELADA, con fin UTC y sin promedio.
6. Nueva sesión desde otra tarjeta del catálogo: POST 201, ID 44 distinto. Cancelación confirmada desde MCK 03 actualiza el panel y permite otra práctica.
7. Creación adicional ID 45 y cancelación confirmada desde MCK 04. El aviso desaparece y se habilitan los botones de inicio. El panel conserva cero completadas. Sesión activa inspeccionada también a 390 px.

Los IDs son evidencia de esta ejecución, no datos precargados en la aplicación. Las capturas auxiliares locales son `.tools/inc3-panel-inicial.png`, `.tools/inc3-sesion-activa.png`, `.tools/inc3-sesion-movil.png` y `.tools/inc3-sesion-cancelada.png`. La herramienta de navegador de Codex devolvió `Transport closed`; se completó la verificación con Chrome local y Playwright instalado solo en `.tools/browser-check`, sin añadir dependencias al proyecto.

El avance hasta preguntas 2/3, la sesión COMPLETADA y su promedio se verifican con fixtures reales de BD en las pruebas de integración. El navegador no graba ni genera evaluaciones: captura/revisión corresponden a RF-07..RF-10 en el Incremento 4; procesamiento y cierre automático son posteriores. La expiración JWT no se volvió a esperar durante 1800 segundos en navegador; su validación automatizada anterior sigue pasando.

## Git y entrega

Antes del cierre se verificó con `git ls-remote --heads origin main` la referencia `e47eeb6affc14b00fbcfc81912e304d145d47819` en `https://github.com/Luayzzhub/entrevista-pro-360.git`. Incremento 1 (`c626aaf`) e Incremento 2 (`e47eeb6`) ya estaban publicados. Tras la auditoría y aprobación del Orquestador, este commit confirma el Incremento 3 con el mensaje exacto `feat: implementar sesiones de entrevista, bloqueo pesimista y navegacion MCK03-MCK04`. Se comprueba que el índice excluya auxiliares, compilados, dependencias y capturas temporales. Después se ejecuta `git push origin main`; el hash resultante y el resultado de publicación se registran en la documentación del Incremento 4. Los informes previos conservan su estado histórico.
