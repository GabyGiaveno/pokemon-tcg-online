# FRONTEND_AUTH_UI_SPEC: Interfaz de Autenticación (Login + Register)

Este documento define la especificación de diseño y estructura visual del módulo de autenticación del frontend para el proyecto Pokémon TCG. Su alcance principal es de **UX/UI y arquitectura de presentación** en Angular, manteniendo desacople de DTOs concretos, y actualmente las pantallas de login/register ya consumen la API de autenticación (`/api/auth/*`) mediante `authService` (`AuthApiService`).

---

## 1. Objetivo

Diseñar una experiencia de autenticación moderna, profesional y visualmente impactante para las pantallas de **Login** y **Register**, con estética inspirada en Pokémon TCG Live / Pokémon TCG Online, pero orientada a una presentación competitiva, gamer y universitaria.

La solución debe quedar preparada para integrar luego servicios JWT reales sin necesidad de reescribir la interfaz.

---

## 2. Alcance

### 2.1 Incluye
- Pantalla de Login.
- Pantalla de Register.
- Layout compartido de autenticación.
- Componentes visuales reutilizables.
- Rutas de frontend para autenticación.
- Estructura de carpetas recomendada.
- Formularios reactivos con validación visual.
- Estados de feedback visual: error, éxito, loading y disabled.
- Integración actual con `authService` (`AuthApiService`) para login/register.

### 2.2 No incluye
- Implementación de backend.
- DTOs definitivos.
- Persistencia de sesión.
- Guards/interceptors funcionales, aunque sí deben quedar contemplados arquitectónicamente.

---

## 3. Principios de Diseño

1. **Desacoplamiento**: la UI no debe depender de nombres de DTOs del backend.
2. **Escalabilidad**: la estructura debe permitir sumar nuevas pantallas de auth sin refactor grande.
3. **Mantenibilidad**: la lógica visual debe separarse de la futura capa de integración.
4. **Accesibilidad**: contraste adecuado, labels claros, navegación por teclado y mensajes legibles.
5. **Estética premium**: evitar un look infantil; priorizar un tono competitivo, elegante y tecnológico.

---

## 4. Stack Visual Recomendado

### 4.1 Tecnologías
- **Angular standalone components**.
- **Angular Material** para inputs, botones, cards, iconografía y estados accesibles.
- **TailwindCSS** para composición visual, gradientes, spacing, blur, sombras y responsividad.
- **SCSS** para capas de estilo más expresivas y animaciones propias.
- **TypeScript estricto**.

### 4.2 Justificación
- Angular Material aporta consistencia, accesibilidad y componentes listos para formulario.
- Tailwind permite lograr una estética gamer/premium sin depender de clases complejas.
- SCSS permite personalizar detalles visuales que den identidad al proyecto.

---

## 5. Estructura Recomendada de Carpetas

La autenticación debe vivir dentro de `FE/src/app/features/auth/`.

```txt
src/app/features/auth/
  auth.routes.ts
  models/
    auth-form.types.ts
    auth-feedback.types.ts
    password-strength.types.ts
  pages/
    login-page/
      login-page.ts
      login-page.html
      login-page.scss
    register-page/
      register-page.ts
      register-page.html
      register-page.scss
  components/
    auth-shell/
      auth-shell.ts
      auth-shell.html
      auth-shell.scss
    auth-card/
      auth-card.ts
      auth-card.html
      auth-card.scss
    auth-logo/
      auth-logo.ts
      auth-logo.html
      auth-logo.scss
    auth-background/
      auth-background.ts
      auth-background.html
      auth-background.scss
    auth-input/
      auth-input.ts
      auth-input.html
      auth-input.scss
    password-strength/
      password-strength.ts
      password-strength.html
      password-strength.scss
    auth-feedback/
      auth-feedback.ts
      auth-feedback.html
      auth-feedback.scss
  services/
    auth-ui.service.ts
```

---

## 6. Rutas

El módulo debe exponer al menos estas rutas:

- `/auth/login`
- `/auth/register`

### 6.1 Reglas de navegación
- La ruta raíz de auth puede redirigir a `/auth/login`.
- El acceso a register debe ser directo y visible desde login.
- Debe existir un enlace visual para volver al login desde register.

---

## 7. Estructura Visual General

### 7.1 Layout base
Cada pantalla debe usar una composición tipo **auth shell**:
- fondo dinámico oscuro,
- capa de partículas o energía,
- card principal centrada,
- panel decorativo con branding Pokémon TCG,
- responsive vertical en mobile,
- versión dual-panel o split en desktop si el espacio lo permite.

### 7.2 Estilo visual
- Tema oscuro predominante.
- Azul eléctrico, amarillo Pokémon, rojo suave y violeta/neón como acentos.
- Glassmorphism o neumorphism moderno.
- Sombras glow/neón suaves.
- Fondo con sensación de cartas, energía o partículas desenfocadas.
- Animaciones sutiles, no invasivas.

### 7.3 Tono
Debe sentirse como una interfaz seria para un TPI universitario, no como una app infantil. La referencia debe ser más cercana a un dashboard gamer competitivo que a un sitio promocional para niños.

---

## 8. Pantalla de Login

### 8.1 Elementos obligatorios
- Campo email/username.
- Campo password.
- Botón principal de login.
- Link/botón para ir a register.
- Remember me.
- Forgot password visual.
- Logo custom de Pokémon TCG.
- Área para mensajes de error y éxito.
- Inputs con iconos.
- Animaciones hover/focus.

### 8.2 Comportamiento visual
- Card central con borde suave y blur.
- Inputs con estado focus visible y glow tenue.
- Botón principal con feedback hover y loading.
- Mensajes de error con estilo claro, no agresivo.
- Link de registro visible pero secundario.

### 8.3 Microinteracciones
- Hover en botones y links.
- Focus ring consistente.
- Transición de entrada de card.
- Pequeña animación en el logo o fondo.

---

## 9. Pantalla de Register

### 9.1 Elementos obligatorios
- Username.
- Email.
- Password.
- Confirm password.
- Avatar opcional.
- Botón crear cuenta.
- Link para volver al login.
- Validaciones visuales modernas.
- Indicador de fortaleza de contraseña.

### 9.2 Comportamiento visual
- Debe reutilizar el mismo lenguaje visual del login.
- La jerarquía debe priorizar completar el registro sin saturar la pantalla.
- El indicador de fortaleza debe ser visual, simple y claro.
- El avatar opcional puede mostrarse como bloque secundario o uploader minimalista.

---

## 10. Componentes Reutilizables

### 10.1 `auth-shell`
Responsable del fondo, layout general y composición general.

### 10.2 `auth-card`
Contenedor visual principal de login/register.

### 10.3 `auth-logo`
Logo custom del módulo auth con identidad Pokémon TCG.

### 10.4 `auth-background`
Capas decorativas: partículas, blur, energía, cartas difuminadas.

### 10.5 `auth-input`
Input reutilizable con icono, error visual y estados de foco.

### 10.6 `password-strength`
Componente para indicar fortaleza de contraseña con feedback visual.

### 10.7 `auth-feedback`
Presentación de errores, éxito o mensajes informativos.

---

## 11. Contratos Genéricos de UI

No se deben usar DTOs concretos del backend.

### 11.1 Tipos sugeridos

```ts
export interface AuthFormState {
  identifier: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterFormState {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  avatarUrl?: string;
}

export interface AuthFeedback {
  type: 'success' | 'error' | 'info';
  message: string;
}

export interface PasswordStrengthState {
  score: number;
  label: string;
  colorClass: string;
}
```

### 11.2 Regla importante
La UI debe trabajar con estos contratos o equivalentes, no con nombres de backend.

---

## 12. Reglas Angular

1. Usar **standalone components**.
2. Usar **Reactive Forms**.
3. Preferir **signals** para estado local de UI.
4. Usar `OnPush` en componentes presentacionales.
5. Mantener componentes pequeños y de responsabilidad única.
6. No mezclar navegación, render y lógica de integración en el mismo componente.
7. Preparar la estructura para lazy loading del feature auth.

---

## 13. Preparación para JWT futuro

La arquitectura debe quedar lista para una integración posterior con backend JWT mediante una capa separada.

### 13.1 Capa prevista
- `AuthService` de integración.
- `AuthMapper` para traducir contratos UI a DTOs reales.
- `AuthInterceptor` para token en requests futuras.
- `AuthGuard` para proteger rutas privadas.

### 13.2 Regla de evolución
Si cambian los DTOs del backend, el cambio idealmente debe afectar solo a:
- mappers,
- servicio de auth,
- tipos de respuesta.

La UI no debería reescribirse.

---

## 14. Validaciones Visuales

### Login
- Email/username obligatorio.
- Password obligatorio.
- Feedback visual de error por campo.

### Register
- Username obligatorio.
- Email válido.
- Password obligatorio.
- Confirm password debe coincidir.
- Indicador de fortaleza visible.

### Reglas de feedback
- Los errores deben ser claros y breves.
- La validación visual no debe romper el layout.
- Los estados inválidos deben ser consistentes en toda la interfaz.

---

## 15. Animaciones y Efectos

Se permiten, pero con moderación.

### Recomendados
- Fade/slide suave de entrada.
- Glow sutil en bordes y botones.
- Movimiento leve del fondo.
- Transiciones de hover/focus.
- Loader elegante para submit.

### No recomendados
- Animaciones excesivas.
- Brillos agresivos o molestos.
- Efectos que afecten la legibilidad.

---

## 16. Responsive Design

### Mobile
- Layout apilado.
- Card principal a ancho completo con márgenes cómodos.
- Fondo decorativo simplificado.
- Botones grandes y accesibles.

### Desktop
- Panel central más ancho.
- Posible vista dual con branding lateral.
- Mayor profundidad visual con blur y capas.

---

## 17. Criterios de Calidad

La implementación se considera alineada con esta SPEC si:
- login y register comparten identidad visual,
- no dependen de DTOs concretos,
- la estructura permite integración futura sin refactor mayor,
- la experiencia luce profesional y consistente,
- la UI es responsive y accesible.

---

## 18. Mejoras Futuras Sugeridas

- Añadir animación de transición entre login y register.
- Incorporar selección de avatar con preview.
- Agregar login con proveedores externos si el proyecto lo requiere.
- Incluir modo de recuperación de contraseña.
- Integrar estados globales de sesión y refresh token.
- Unificar este módulo con un auth layout para futuras pantallas de perfil o onboarding.

---

## 19. Resumen de Decisión Arquitectónica

La decisión central de esta SPEC es separar la **presentación** de la **integración con backend**. Esto permite que el frontend de autenticación se mantenga estable aunque cambien los DTOs o el contrato JWT en el futuro.

---
