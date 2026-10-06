import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthTokenService } from '../data-access/auth-token.service';
import { LobbyApiService } from '../data-access/lobby-api.service';
import { GameSessionResponse } from '../data-access/lobby-api.types';
import { AuthFeedback } from '../models/auth-feedback.types';
import { ProfileApiService } from '../../profile/data-access/profile-api.service';
import { PlayerProfileResponse, ProfileStats } from '../../profile/data-access/profile-api.types';
import { HoverSoundDirective } from '../../../shared/directives/hover-sound.directive';
import { SettingsMenu } from '../../../shared/components/settings-menu/settings-menu';
import { BackgroundMusicService } from '../../../shared/services/background-music.service';
import { NewsService } from '../../../shared/services/news.service';
import { NewsItem } from '../../../shared/models/news.model';
import { DeckApi } from '../../deck-builder/data-access/deck-api.service';
import { DECK_BUILDER_API_PROVIDERS } from '../../deck-builder/data-access/deck-builder-api.provider';
import { DeckCardResponse, DeckResponse } from '../../deck-builder/models/deck.model';

@Component({
  selector: 'app-lobby-page',
  imports: [RouterLink, HoverSoundDirective, SettingsMenu],
  providers: [...DECK_BUILDER_API_PROVIDERS],
  templateUrl: './lobby.html',
  styleUrl: './lobby.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LobbyPage implements OnDestroy {
  private readonly api = inject(LobbyApiService);
  private readonly profileApi = inject(ProfileApiService);
  private readonly authToken = inject(AuthTokenService);
  private readonly router = inject(Router);
  private readonly deckApi = inject(DeckApi);
  private readonly backgroundMusic = inject(BackgroundMusicService);
  private readonly newsService = inject(NewsService);

  protected readonly feedback = signal<AuthFeedback | null>(null);
  protected readonly profile = signal<PlayerProfileResponse | null>(null);
  protected readonly stats = signal<ProfileStats | null>(null);
  protected readonly showGamePanel = signal(false);
  protected readonly games = signal<GameSessionResponse[]>([]);
  protected readonly loading = signal(false);

  protected readonly selectedDeckId = signal<number | null>(null);
  protected readonly decks = signal<DeckResponse[]>([]);
  protected readonly decksLoading = signal(false);
  protected readonly validDecks = computed(() =>
    this.decks().filter((d) => d.valid),
  );

  protected readonly news = signal<NewsItem[]>([]);
  protected readonly newsIndex = signal(0);
  protected readonly currentNews = computed(() => this.news()[this.newsIndex()] ?? null);
  private newsTimer: ReturnType<typeof setInterval> | null = null;

  /** Wizard step inside the play panel: 1 = pick a deck, 2 = pick/create a game. */
  protected readonly gameStep = signal<1 | 2>(1);

  /** The currently selected deck object (used to show its name in step 2). */
  protected readonly selectedDeck = computed(() =>
    this.decks().find((d) => d.id === this.selectedDeckId()) ?? null,
  );

  protected firstPreviewCard(deck: DeckResponse): DeckCardResponse | null {
    return deck.cards.find((card) => !!card.imageUrlSmall) ?? null;
  }

  /** Step 1 → 2: advance to the games panel. Requires a selected deck. */
  protected goToGames(): void {
    if (this.selectedDeckId() === null) {
      return;
    }
    this.gameStep.set(2);
    this.loadGames();
  }

  /** Step 2 → 1: go back to re-pick the deck (keeps the current selection). */
  protected backToDeckSelect(): void {
    this.feedback.set(null);
    this.gameStep.set(1);
  }

  constructor() {
    this.loadProfileStats();
    this.loadGames();
    this.loadDecks();
    this.loadNews();
    this.backgroundMusic.play();
  }

  ngOnDestroy(): void {
    this.stopNewsTimer();
  }

  protected goToNews(index: number): void {
    this.newsIndex.set(index);
    this.restartNewsTimer();
  }

  protected prevNews(): void {
    const total = this.news().length;
    if (total === 0) return;
    this.newsIndex.update((i) => (i - 1 + total) % total);
    this.restartNewsTimer();
  }

  protected nextNews(): void {
    const total = this.news().length;
    if (total === 0) return;
    this.newsIndex.update((i) => (i + 1) % total);
    this.restartNewsTimer();
  }

  private loadNews(): void {
    this.newsService.getNews().subscribe({
      next: (items) => {
        this.news.set(items);
        this.startNewsTimer();
      },
    });
  }

  private startNewsTimer(): void {
    this.stopNewsTimer();
    if (this.news().length > 1) {
      this.newsTimer = setInterval(() => this.nextNews(), 6000);
    }
  }

  private restartNewsTimer(): void {
    this.startNewsTimer();
  }

  private stopNewsTimer(): void {
    if (this.newsTimer !== null) {
      clearInterval(this.newsTimer);
      this.newsTimer = null;
    }
  }

  protected get playerName(): string {
    return this.profile()?.username ?? this.authToken.usernameValue ?? 'Entrenador';
  }

  protected get playerXp(): number {
    return this.profile()?.xpPercent ?? 65;
  }

  protected toggleGamePanel(): void {
    if (this.showGamePanel()) {
      this.feedback.set(null);
    }
    this.showGamePanel.update(v => !v);
    if (this.showGamePanel()) {
      // Always start the wizard at step 1 (deck selection).
      this.gameStep.set(1);
      this.loadDecks();
    }
  }

  protected loadGames(): void {
    this.loading.set(true);
    this.api.getWaitingGames()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (games) => {
          this.games.set(games);
          this.feedback.set(games.length
            ? { type: 'success', message: `Hay ${games.length} partida(s) disponibles.` }
            : { type: 'info', message: 'No hay partidas esperando jugadores.' }
          );
        },
        error: () => {
          this.feedback.set({ type: 'error', message: 'No se pudo conectar al servidor.' });
        }
      });
  }

  protected get availableGamesCount(): number {
    return this.games().length;
  }

  private loadDecks(): void {
    this.decksLoading.set(true);
    this.deckApi.getDecks()
      .pipe(finalize(() => this.decksLoading.set(false)))
      .subscribe({
        next: (decks) => this.decks.set(decks),
        error: () => this.decks.set([]),
      });
  }

  private loadProfileStats(): void {
    this.profileApi.getProfile().subscribe({
      next: (profile) => this.profile.set(profile),
      error: () => this.profile.set(null),
    });

    this.profileApi.getStats().subscribe({
      next: (stats) => this.stats.set(stats),
      error: () => this.stats.set(null),
    });
  }

  protected createGame(): void {
    const deckId = this.selectedDeckId();
    if (deckId === null) {
      this.feedback.set({ type: 'error', message: 'Seleccioná un mazo válido.' });
      return;
    }
    this.loading.set(true);
    this.api.createGame(deckId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          void this.router.navigateByUrl(`/game/${response.gameId}`);
        },
        error: () => {
          this.feedback.set({ type: 'error', message: 'No se pudo crear la partida.' });
        }
      });
  }

  protected joinGame(gameId: string): void {
    const deckId = this.selectedDeckId();
    if (deckId === null) {
      this.feedback.set({ type: 'error', message: 'Seleccioná un mazo válido.' });
      return;
    }
    this.loading.set(true);
    this.api.joinGame(gameId, deckId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          void this.router.navigateByUrl(`/game/${response.gameId}`);
        },
        error: () => {
          this.feedback.set({ type: 'error', message: 'No se pudo unir a la partida.' });
        }
      });
  }

  protected logout(): void {
    this.authToken.clear();
    void this.router.navigateByUrl('/auth/login');
  }
}
