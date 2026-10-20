# API de identidad — Incremento 2

Prefijo `/api/v1`, JSON UTF-8 y nombres `snake_case`. Las respuestas nunca contienen `password` ni `password_hash`. Fechas de usuario: UTC ISO 8601 (`Z`). Errores: `codigo`, `mensaje`, `errores_campo`.

## POST /auth/registro

Público. Entrada cerrada: `nombre_completo`, `email`, `password`.

```json
{"nombre_completo":"Ana Pérez","email":"ana@example.test","password":"contraseña de ejemplo 2026"}
```

201 devuelve `id_usuario`, `email`, `nombre_completo`, `rol`, `estado`, `fecha_registro`, `fecha_actualizacion`, `resumen_perfil`. Rol POSTULANTE, estado ACTIVO y fechas se asignan en servidor. No inicia sesión automáticamente. 400 señala campos inválidos/no permitidos; 409 identifica correo ya registrado.

## POST /auth/login

Público. Entrada cerrada: `email`, `password`. El correo se normaliza; el password se compara exactamente, incluidos los espacios.

200 devuelve `access_token`, `token_type: "Bearer"`, `expires_in: 1800` y `usuario` con las ocho propiedades públicas. JWT HS256 con sub/rol/iat/exp e issuer. 400 identifica datos inválidos; 401 usa un mensaje común para credenciales inválidas o cuenta no habilitada. No hay refresh token ni renovación automática.

## GET /perfil

Privado: `Authorization: Bearer <token>`. Devuelve las ocho propiedades públicas del usuario autenticado. Un `id_usuario` en parámetros no selecciona otro perfil: el propietario proviene exclusivamente del contexto de seguridad.

## PATCH /perfil

Privado. Entrada cerrada: `nombre_completo` obligatorio y `resumen_perfil` opcional/null. El formulario envía ambos campos. Un resumen ausente, null o blanco se guarda como null.

```json
{"nombre_completo":"Ana Pérez Actualizada","resumen_perfil":"Estudiante de Ingeniería de Sistemas."}
```

200 devuelve el perfil actualizado. Solo cambian nombre, resumen y fecha de actualización. Correo, ID, hash, rol, estado y registro permanecen intactos. Enviar `email`, `rol`, `estado`, `id_usuario`, `password_hash` o cualquier propiedad desconocida produce 400 `CAMPOS_NO_PERMITIDOS`.

## Validación

- Nombre: trim, 1..150 caracteres.
- Correo: trim, minúsculas Locale.ROOT, formato válido y máximo 150 caracteres.
- Password: mínimo 12 puntos de código Unicode y máximo 72 bytes UTF-8, sin trim ni normalización silenciosa. También se validan estos límites en login.
- Resumen: trim, vacío a null, máximo 500 y texto plano. El servidor no interpreta HTML y Angular no lo inserta como HTML.
- Propiedades desconocidas, tipos incompatibles o JSON malformado: 400.

401 privado cubre ausencia de token, firma/algoritmo incorrecto, vencimiento y cuenta inexistente/no activa/rol incompatible. No se exponen detalles criptográficos ni hashes. El cierre de sesión borra el estado en memoria del cliente; no hay endpoint de logout ni revocación inmediata.
