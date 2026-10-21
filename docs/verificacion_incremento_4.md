# Verificación del Incremento 4

Fecha: 21/10/2026, America/Lima. Alcance: RF-07, RF-08, RF-09 en cliente y RF-10; MCK 05 preparación, MCK 06 captura y MCK 07 revisión/repetición local. El Orquestador auditó y aprobó el Incremento 4, confirmado en este commit de main con el mensaje `feat: implementar permisos de camara y microfono, captura de 60s y revision local MCK05-MCK07`. Se excluyen auxiliares, compilados y medios temporales; la publicación se comprueba con push y ls-remote antes de iniciar el Incremento 5.

## Cierre previo del Incremento 3

Antes de escribir la captura local se actualizaron las notas de README, matriz, verificación y JSON del Incremento 3 para reflejar su auditoría/aprobación. Se verificó que el índice no contuviera `.tools/`, `target/`, `dist/`, `node_modules/`, capturas ni medios temporales.

Se creó en main el commit **`214c011c7b13e0c365dbac33d592dd1661347001`**, mensaje exacto:

```text
feat: implementar sesiones de entrevista, bloqueo pesimista y navegacion MCK03-MCK04
```

`git push origin main` terminó con código 0 y salida:

```text
To https://github.com/Luayzzhub/entrevista-pro-360.git
   e47eeb6..214c011  main -> main
```

`git ls-remote --heads origin main` confirmó `214c011c7b13e0c365dbac33d592dd1661347001 refs/heads/main`. El árbol quedó limpio después de confirmar ese incremento y antes de construir el 4. Los hashes del commit y evidencias anteriores se conservan; no se reescribe el historial.

## Construcción y regresión

| Comprobación | Resultado |
| --- | --- |
| `mvn test` — H2 | 73 casos descubiertos, **71 ejecutados**, 2 exclusivos de MySQL omitidos, 0 fallos, 0 errores |
| `mvn test -Dspring.profiles.active=mysql` — MySQL 8.0.44 | **73 ejecutados**, 0 omitidos, 0 fallos, 0 errores |
| Pureza de capas y concurrencia | `LayerDependenciesTest` y dos POST HTTP simultáneos siguen pasando en los motores correspondientes |
| `npm run test:media` | **6 pruebas correctas**, 0 fallos/omitidas; negociación MIME y bordes de duración/tamaño |
| `npm run build` | Producción AOT/plantillas estrictas correctas; **0 advertencias de presupuesto**; inicial 286.53 kB, transferencia estimada 78.45 kB |
| JAR | `mvn package -DskipTests` correcto y ejecución contra MySQL real |
| Navegador | **17 escenarios correctos**, 0 errores JavaScript; Chrome 155.0.8059.85 + Playwright 1.64.1 |
| Esquema | V1/V2 idénticas a HEAD, 8 tablas/68 columnas de negocio, ddl-auto=validate |

No se modificó código/configuración/pruebas del backend en Incremento 4. No se añadió ninguna dependencia Maven/npm ni se modificó package-lock; solo se agregó el comando de tests de medios a package.json. Las Web APIs nativas implementan la captura. Los tests puros ejecutan las reglas TypeScript usando el compilador existente y node:test.

Entorno aislado: base `ep360_incremento4_test`, MySQL en `127.0.0.1:33307`, JAR en 18080 y Angular en 14200. Los logs/scripts/capturas locales de `.tools/` están ignorados. El JSON versionable [pruebas-incremento-4.json](evidencias/pruebas-incremento-4.json) conserva resultados, suites/casos, observaciones de navegador y SHA-256 de sus fuentes locales.

## Recorrido y medios reales

Chrome se ejecutó con `--use-fake-device-for-media-stream` y `--use-fake-ui-for-media-stream` para obtener pistas de hardware emulado. El MediaStream, MediaRecorder, Blob, URLs y reproductor son los nativos de Chrome; no se sustituyeron respuestas HTTP ni se precargaron videos como resultado de la aplicación. Para denegación se usó otra instancia sin fake UI con permisos bloqueados por el navegador.

1. **Preparación:** login y sesión real; aviso S3/STT/IA visible; inicio deshabilitado antes del permiso. Ambas pistas live/enabled, indicadores Lista/Listo, preview con imagen y muted=true, MIME VP8/Opus WebM explícito.
2. **Grabación/revisión:** toma manual de más de un segundo; se reprodujo la URL blob con imagen/audio y se comprobaron ambas pistas en el stream del reproductor. Primera toma: 2.409 s registrados, 133172 bytes; confirmación deshabilitada y aviso del Incremento 5.
3. **Repetición:** se revocó inmediatamente la primera URL, se prepararon otra vez los dispositivos y se grabó una segunda toma real de la misma pregunta/sesión, con URL distinta. MySQL seguía con cero respuestas.
4. **Duración mínima:** toma inmediata inferior a un segundo rechazada, con advertencia de mínimo y botón para repetir; confirmar deshabilitado.
5. **Borde de 60 segundos:** se esperaron 60 segundos reales, sin avanzar/simular performance.now. El intervalo medido entre start y solicitud de stop fue **60000.6 ms**. La UI presentó límite alcanzado, 60.000 s registrados y Blob consolidado de 3477152 bytes.
6. **Salida:** durante grabación, volver a panel dejó las pistas ended y los recorders inactivos. Las URLs anteriores estaban revocadas.
7. **Denegación real:** permisos rechazados por Chrome, explicación en español, inicio bloqueado y reintento disponible.
8. **Sin formato compatible:** isTypeSupported respondió false a los tres tipos, en el orden contractual; incompatibilidad explícita y ningún recorder construido.
9. **Sin MediaRecorder:** capacidad ausente emulada en el navegador; incompatibilidad e inicio bloqueado.
10. **Sin getUserMedia:** capacidad ausente emulada en el navegador; incompatibilidad e inicio bloqueado.
11. **Pista parcial:** getUserMedia nativo entregó un stream al que la prueba retiró la pista de audio; el controlador rechazó la preparación y detuvo también el video parcial.
12. **Fallo al iniciar:** excepción de start inyectada en la prueba; mensaje claro, fragmentos descartados y medios liberados.
13. **Desconexión:** la prueba detuvo una pista de audio y emitió ended durante captura; se descartó la toma y se liberaron las demás pistas.
14. **Error de recorder:** evento error inyectado durante una captura nativa; sin revisión de fragmentos incompletos y dispositivos apagados.
15. **Permiso tardío:** se retrasó la resolución del getUserMedia nativo, se abandonó la vista y se resolvió después; todas las pistas se apagaron.
16. **Cerrar sesión:** salir desde preparación con medios activos liberó las pistas y volvió a login.
17. **Cancelar práctica:** cancelación real desde una grabación retiró la captura, descartó fragmentos y detuvo las pistas.

Los escenarios 8–15 usan instrumentación controlada de capacidades/eventos/fallos en la prueba de navegador; no certifican una desconexión física ni soporte en todos los navegadores. No hay dobles de MediaRecorder para producir videos falsos en el camino normal. El audio y video reproducidos fueron generados por la captura nativa de los dispositivos Chromium emulados. La revisión se inspeccionó en escritorio y a 390 px, sin desborde horizontal; capturas auxiliares: `.tools/inc4-preparacion.png`, `.tools/inc4-revision.png`, `.tools/inc4-revision-movil.png`.

## Persistencia y límites de aceptación

La consulta directa `SELECT COUNT(*) FROM ep360_incremento4_test.respuestas_video` devolvió **0 antes, 0 después de repetir y 0 al finalizar**. La captura no envió PUT/POST de respuesta ni solicitudes S3. Solo se escribieron cuenta/sesión de prueba y su cancelación mediante los contratos existentes. La pregunta siguió siendo la 1; no se inventan respuestas confirmadas ni evaluaciones.

RF-07/RF-08/RF-10 quedan Verificados en su alcance local. RF-09 queda **En progreso**: reloj/plazo y validación local correctos; la duración mostrada es el intervalo solicitado de captura y no una inspección del contenedor. El timeout de JavaScript pide stop al alcanzar el plazo; la observación de 60000.6 ms refleja la latencia de planificación real. No se certifica precisión física exacta del contenedor mediante este reloj. ffprobe en servidor, confirmación multipart idempotente y subida privada S3 pertenecen al Incremento 5. Confirmar permanece deshabilitado para todas las tomas en este incremento.

Las reglas puras verifican el orden/fallback VP8→VP9→MP4, incompatibilidad, 1/60 s inclusivos, rechazo de 0.999/60.001/no finitos, 0 y 50 000 000 bytes inválidos, 49 999 999 válido y acumulación de errores. La captura normal E2E usó VP8/Opus WebM; no se afirma una grabación E2E de VP9/MP4 en este equipo.
