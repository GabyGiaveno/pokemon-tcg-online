import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HoverSoundDirective } from '../../../../shared/directives/hover-sound.directive';
import { SettingsMenu } from '../../../../shared/components/settings-menu/settings-menu';
import { BackgroundMusicService } from '../../../../shared/services/background-music.service';

interface RuleSection {
  title: string;
  icon: string;
  content: string[];
}

@Component({
  selector: 'app-rules-page',
  imports: [HoverSoundDirective, SettingsMenu],
  templateUrl: './rules-page.html',
  styleUrl: './rules-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RulesPage {
  private readonly router = inject(Router);
  private readonly backgroundMusic = inject(BackgroundMusicService);

  protected readonly activeSection = signal(0);

  protected readonly sections: RuleSection[] = [
    {
      title: '¿Qué es Pokémon TCG?',
      icon: '🃏',
      content: [
        'El Pokémon Trading Card Game (TCG) es uno de los juegos de cartas coleccionables más populares del mundo.',
        'Dos jugadores se enfrentan con mazos de 60 cartas construidos estratégicamente.',
        'Cada jugador dispone de Pokémon para combatir, Energías para potenciarlos y cartas de Entrenador para obtener ventajas tácticas.',
        'El objetivo es vencer a los Pokémon del oponente para acumular cartas de Premio, agotar el mazo contrario, o dejar al rival sin Pokémon en juego.',
      ],
    },
    {
      title: 'Preparación de la partida',
      icon: '🎴',
      content: [
        'Cada jugador baraja su mazo de 60 cartas y roba 7 cartas como mano inicial.',
        'Se colocan 6 cartas de Premio boca abajo a un costado. Cada vez que noquees un Pokémon rival, tomás una carta de Premio.',
        'Elegí un Pokémon Básico de tu mano y colocalo boca abajo como tu Pokémon Activo.',
        'Podés colocar hasta 5 Pokémon Básicos adicionales en tu Banca (bench).',
        'Si no tenés ningún Pokémon Básico en tu mano inicial, mostrás tu mano, barajás y volvés a robar. Tu rival puede robar una carta extra por cada mulligan.',
      ],
    },
    {
      title: 'Tipos de cartas',
      icon: '📋',
      content: [
        'Pokémon: Son las criaturas que combaten. Pueden ser Básicos, Fase 1 (evolucionan de un Básico) o Fase 2 (evolucionan de Fase 1). Tienen PS (Puntos de Salud), ataques, debilidad, resistencia y costo de retirada.',
        'Energía: Se adjuntan a los Pokémon para que puedan usar sus ataques. Solo podés adjuntar 1 Energía por turno. Hay Energías básicas (Fuego, Agua, Planta, Eléctrico, Psíquico, Lucha, Oscuridad, Metal, Hada) y Energías especiales con efectos adicionales.',
        'Entrenador: Cartas de soporte que te dan ventajas tácticas. Se dividen en Objetos (usás cuantos quieras por turno), Partidarios/Supporter (solo 1 por turno) y Estadios (permanecen en juego, solo 1 a la vez).',
      ],
    },
    {
      title: 'Estructura del turno',
      icon: '🔄',
      content: [
        '1. Robar una carta: Al inicio de tu turno, robás una carta de tu mazo. Si no podés robar, perdés la partida.',
        '2. Acciones (en cualquier orden y cuantas veces quieras): Colocar Pokémon Básicos en la Banca, adjuntar 1 Energía a un Pokémon, evolucionar Pokémon (no en el primer turno ni el turno en que se jugó), jugar cartas de Entrenador, retirarte (pagando el costo de retirada) y usar Habilidades (Abilities).',
        '3. Atacar: Elegí uno de los ataques de tu Pokémon Activo (si tiene la Energía necesaria). Aplicá daño y efectos. Esto termina tu turno.',
      ],
    },
    {
      title: 'Combate y daño',
      icon: '⚔️',
      content: [
        'Para atacar, tu Pokémon Activo necesita tener adjuntada la Energía que indica el costo del ataque.',
        'El daño se aplica al Pokémon Activo del rival. Colocá contadores de daño sobre él.',
        'Debilidad: Si el Pokémon atacante es del tipo al que el defensor es débil, el daño se duplica (×2).',
        'Resistencia: Si el defensor resiste ese tipo, se resta 30 puntos de daño.',
        'Cuando un Pokémon acumula daño igual o mayor a sus PS, queda Noqueado. Va al descarte junto con todas sus cartas adjuntas, y el rival toma una carta de Premio.',
      ],
    },
    {
      title: 'Evolución',
      icon: '🔺',
      content: [
        'Los Pokémon evolucionan colocando la carta de evolución encima del Pokémon en juego.',
        'Un Pokémon Básico evoluciona a Fase 1, y un Fase 1 evoluciona a Fase 2.',
        'No podés evolucionar un Pokémon el mismo turno en que lo jugaste, ni en tu primer turno.',
        'Podés evolucionar Pokémon tanto del Activo como de la Banca.',
        'Al evolucionar, se curan los estados especiales (Envenenado, Dormido, etc.) pero el daño permanece.',
      ],
    },
    {
      title: 'Retirada y Banca',
      icon: '🔁',
      content: [
        'Podés retirar a tu Pokémon Activo una vez por turno pagando su costo de retirada (descartando Energías adjuntas).',
        'Al retirarte, elegís un Pokémon de tu Banca para que pase a ser el nuevo Activo.',
        'Si tu Pokémon Activo es noqueado, debés promover uno de tu Banca. Si no tenés Pokémon en la Banca, perdés.',
        'La Banca puede tener hasta 5 Pokémon. Algunos ataques y habilidades pueden afectar a los Pokémon en la Banca del rival.',
      ],
    },
    {
      title: 'Condiciones de victoria',
      icon: '🏆',
      content: [
        'Ganás la partida si cumplís alguna de estas tres condiciones:',
        '1. Tomaste las 6 cartas de Premio — noqueaste suficientes Pokémon rivales para reclamar todos tus premios.',
        '2. Tu rival no tiene Pokémon en juego — noqueaste su Pokémon Activo y no le queda ningún Pokémon en la Banca.',
        '3. Tu rival no puede robar carta — al inicio de su turno, su mazo está vacío.',
      ],
    },
    {
      title: 'Estados especiales',
      icon: '💫',
      content: [
        'Envenenado: Entre turnos, el Pokémon recibe 1 contador de daño (10 PS). Se marca con un marcador de veneno.',
        'Quemado: Entre turnos, se lanza una moneda. Si sale cruz, recibe 2 contadores de daño (20 PS).',
        'Dormido: El Pokémon no puede atacar ni retirarse. Entre turnos, se lanza una moneda. Si sale cara, se despierta.',
        'Paralizado: El Pokémon no puede atacar ni retirarse durante un turno. Se cura al final del turno del jugador afectado.',
        'Confundido: Al atacar, se lanza una moneda. Si sale cruz, el Pokémon se hace 30 de daño a sí mismo en vez de atacar.',
      ],
    },
  ];

  constructor() {
    this.backgroundMusic.play();
  }

  protected selectSection(index: number): void {
    this.activeSection.set(index);
  }

  protected goBack(): void {
    void this.router.navigateByUrl('/lobby');
  }
}
