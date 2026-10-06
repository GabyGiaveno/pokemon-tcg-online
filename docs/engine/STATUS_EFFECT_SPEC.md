# STATUS_EFFECTS_SPEC: Lógica de Condiciones Especiales

El motor debe procesar los efectos en este orden jerárquico estricto.

## 1. Definición de Estados
El `StatusEffectManager` debe procesar los efectos en este orden:
1. **Envenenado**: 1 contador de daño.
2. **Quemado**: Lanzar moneda. Cruz = 2 contadores de daño.
3. **Dormido**: Lanzar moneda. Cara = Despierta.
4. **Paralizado**: Se cura automáticamente al final del turno del jugador afectado.
5. **Confundido**: Lanzar moneda ANTES de atacar. Cruz = falla y recibe 30 de daño.

## 2. Reglas de Acumulación
- **Giro (Exclusivos)**: Dormido, Confundido y Paralizado se pisan entre sí. El más reciente es el único activo.
- **Marcadores (Acumulables)**: Quemado y Envenenado pueden coexistir con cualquier otro estado.
- **Curación**: Moverse a la Banca o Evolucionar elimina TODOS los estados.