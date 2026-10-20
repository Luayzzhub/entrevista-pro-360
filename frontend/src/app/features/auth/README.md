# Identidad — Incremento 2

`/registro` y `/login` usan formularios reactivos y la API real. `core/AuthService` conserva token y usuario exclusivamente en memoria, limpia la sesión a los 1800 s y al cerrar sesión. Las rutas públicas exclusivas redirigen al panel cuando existe sesión vigente.
