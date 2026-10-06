# RECUPERACION_CONTRASENA_SPEC: Restablecimiento de contraseña

## Propósito
Definir un flujo profesional de recuperación y restablecimiento de contraseña para Pokémon TCG, alineado con el auth actual, usando email y token temporal de un solo uso.

## Requisitos

### Requisito: Solicitud de recuperación por email
El sistema MUST permitir que el usuario solicite recuperación ingresando su email en `/auth/forgot-password`.
La respuesta MUST ser genérica y NO debe revelar si el correo existe.

#### Escenario: Email existente
- GIVEN el usuario ingresa un email válido
- WHEN envía la solicitud
- THEN el backend genera un token temporal y envía un correo con enlace de restablecimiento
- AND el frontend muestra un mensaje genérico de confirmación

#### Escenario: Email inexistente
- GIVEN el usuario ingresa un email no registrado
- WHEN envía la solicitud
- THEN el backend responde con el mismo mensaje genérico
- AND no expone si la cuenta existe

### Requisito: Restablecimiento con token
El sistema MUST permitir definir una nueva contraseña usando un token de un solo uso y con expiración.
El enlace enviado por correo MUST dirigir a `/auth/reset-password?token=...`.

#### Escenario: Token válido
- GIVEN el usuario abre el enlace con token vigente
- WHEN ingresa y confirma una nueva contraseña
- THEN el backend valida el token y actualiza la contraseña
- AND invalida el token luego de usarse

#### Escenario: Token vencido o inválido
- GIVEN el usuario abre un enlace roto, vencido o ya usado
- WHEN intenta continuar
- THEN el backend rechaza la operación
- AND el frontend muestra un error claro y seguro

### Requisito: Componentes y dependencias
El frontend SHOULD reutilizar el layout actual de auth y el componente `forgot-password` ya creado.
El backend SHOULD exponer `POST /api/auth/forgot-password` y `POST /api/auth/reset-password`.
El backend MUST depender de un servicio de correo (Spring Mail o equivalente) y de un almacenamiento del token de recuperación con expiración.

#### Escenario: Integración de UI
- GIVEN el usuario navega al flujo de recuperación
- WHEN usa el formulario de forgot password o reset password
- THEN la UI mantiene la estética del auth existente
- AND no rompe el contrato visual de login/register

## Notas de diseño
- El mail de recuperación debe incluir solo el enlace y no la contraseña.
- El mensaje visible al usuario debe ser neutral para evitar enumeración de cuentas.
- El front puede implementar una pantalla adicional de `reset-password` si el token llega por query param.
- El cambio de contraseña directa por username NO se recomienda por seguridad y profesionalismo.

## Tareas a seguir

### Frontend
- Crear `FE/src/app/features/auth/reset-password/` con pantalla standalone para ingresar nueva contraseña y confirmación.
- Mantener `FE/src/app/features/auth/forgot-password/` como pantalla de solicitud de recuperación por email.
- Agregar rutas en `FE/src/app/features/auth/auth.routes.ts` para `/forgot-password` y `/reset-password`.
- Crear `FE/src/app/features/auth/data-access/password-reset-api.service.ts` para consumir `POST /api/auth/forgot-password` y `POST /api/auth/reset-password`.
- Reutilizar `AuthShell`, `AuthFeedback` y el estilo visual actual del auth para ambas pantallas.
- Mapear el `token` desde la query string en `reset-password` y enviarlo junto con la nueva contraseña.
- Mostrar mensajes genéricos de éxito/error sin revelar si el correo existe.

### Backend
- Crear DTOs de recuperación en `BE/src/main/java/.../dtos/request/` para `ForgotPasswordRequest` y `ResetPasswordRequest`.
- Crear un DTO de respuesta genérica si hace falta para mensajes de confirmación.
- Agregar endpoints en `BE/src/main/java/.../controllers/AuthController.java` para `POST /api/auth/forgot-password` y `POST /api/auth/reset-password`.
- Implementar un servicio de recuperación en `BE/src/main/java/.../services/` para generar, guardar, validar y expirar tokens.
- Persistir el token de recuperación con expiración y marca de uso único.
- Integrar un servicio de email (Spring Mail o equivalente) para enviar el enlace de restablecimiento.
- Configurar properties/variables de entorno del mailer en el backend.
- Hacer que el backend responda de forma genérica en `forgot-password` aunque el email no exista.

## Estructura de archivos

### Backend

```txt
BE/src/main/java/ar/edu/utn/frc/tup/piii/
  controllers/
    AuthController.java                        ← agregar POST /forgot-password y POST /reset-password
  dtos/
    request/
      ForgotPasswordRequest.java              ← { email: String }
      ResetPasswordRequest.java               ← { token: String, newPassword: String }
    response/
      PasswordResetResponse.java              ← { message: String }
  entities/
    RecoveryToken.java                        ← nueva entidad (player_id, token, expires_at, used)
  repositories/
    RecoveryTokenRepository.java              ← nueva interfaz JPA
  services/
    RecoveryService.java                      ← generar token, validar, expirar, cambiar password
  mail/
    EmailService.java                         ← enviar mail con Spring Mail

BE/src/main/resources/
  templates/
    recovery-email.html                       ← template HTML del correo
  application.yml                             ← propiedades de mail + host frontend
```

### Frontend

```txt
FE/src/app/features/auth/
  forgot-password/
    forgot-password.ts                        ← ya existe, conectar al endpoint
    forgot-password.html                      ← ajustar si hace falta
    forgot-password.css                       ← ajustar si hace falta
  reset-password/
    reset-password.ts                         ← nuevo: leer token, form, submit
    reset-password.html                       ← nuevo: token oculto + nueva contra + confirmación
    reset-password.css                        ← nuevo: copiar estilo del auth existente
  data-access/
    password-reset-api.service.ts             ← nuevo: consume /forgot-password y /reset-password
  models/
    password-reset.types.ts                   ← nuevo: interfaces request/response
  auth.routes.ts                              ← agregar /reset-password
```
