# Prompt para configurar el proyecto con IA

Usa este prompt con una IA para dejar el entorno local listo:

```text
Necesito que revises mi proyecto Pokémon TCG y me dejes el setup local listo para que el equipo lo use con la menor configuración posible.

Quiero que entiendas esto:
- `.env.example` es la plantilla versionada
- `.env` es el archivo real local y no se commitea
- `application-local.example.yml` es la plantilla versionada
- `application-local.yml` real es local y no se commitea
- `application-prod.yml` es la config para PostgreSQL + mail real
- `run-local.ps1` debe soportar `local` y `prod`

Objetivo:
- arrancar el backend con H2 por defecto
- permitir arrancar con PostgreSQL usando Docker Compose
- usar Gmail para recuperación de contraseña
- no commitear secretos
- dejar el arranque local lo más automático posible para el equipo

Tareas concretas:
1. Verificá que exista `.env.example` en la raíz.
2. Mantené `.env.example` solo como plantilla, sin secretos reales.
3. Verificá que exista `BE/src/main/resources/application-local.example.yml`.
4. Mantené `application-local.example.yml` solo como plantilla, con placeholders para mail.
5. Verificá que exista `BE/src/main/resources/application-prod.yml` y que use PostgreSQL + variables de entorno para mail y JWT.
6. Hacé que `BE/src/main/resources/application-local.yml` exista solo como archivo local y quede fuera de git.
7. Hacé que `run-local.ps1`:
   - cree `.env` copiando `.env.example` si no existe
   - cargue `.env`
   - cree `application-local.yml` copiando la plantilla si no existe
   - arranque `BE` con `-Profile local` por defecto
   - levante `docker compose` y arranque `BE` con `-Profile prod` cuando se pida PostgreSQL
8. Actualizá `README.md` con los dos modos de arranque y el flujo de recuperación de contraseña.

Restricciones:
- no pongas credenciales reales en archivos versionados
- no borres cambios funcionales existentes
- mantené compatibilidad con Windows PowerShell

Al final, devolveme un resumen breve de los archivos cambiados y los comandos exactos para ejecutar el backend en ambos perfiles.
```
