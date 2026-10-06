import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { GameConfirmDialog } from '../../components/game-confirm-dialog/game-confirm-dialog';
import { DeckApi } from '../../data-access/deck-api.service';
import { DECK_BUILDER_API_PROVIDERS } from '../../data-access/deck-builder-api.provider';
import { DeckCardResponse, DeckResponse } from '../../models/deck.model';
import { SettingsMenu } from '../../../../shared/components/settings-menu/settings-menu';
import { BackgroundMusicService } from '../../../../shared/services/background-music.service';

@Component({
  selector: 'app-deck-list-page',
  imports: [DatePipe, RouterLink, GameConfirmDialog, SettingsMenu],
  templateUrl: './deck-list-page.html',
  styleUrl: './deck-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [...DECK_BUILDER_API_PROVIDERS],
})
export class DeckListPage implements OnInit {
  private readonly deckApi = inject(DeckApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly backgroundMusic = inject(BackgroundMusicService);

  // ── State ──────────────────────────────────────────────

  readonly decks = signal<DeckResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly validatingDeckId = signal<number | null>(null);
  readonly validationMessages = signal<Record<number, string>>({});
  readonly deletingDeckId = signal<number | null>(null);
  readonly deckPendingDelete = signal<DeckResponse | null>(null);

  // ── Lifecycle ───────────────────────────────────────────

  ngOnInit(): void {
    this.loadDecks();
    this.backgroundMusic.play();
  }

  // ── Data loading ───────────────────────────────────────

  private loadDecks(): void {
    this.loading.set(true);
    this.error.set(null);

    this.deckApi.getDecks()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (decks) => {
          this.decks.set(decks);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('No se pudieron cargar los mazos.');
          this.loading.set(false);
        },
      });
  }

  protected onRetry(): void {
    this.loadDecks();
  }

  protected previewCards(deck: DeckResponse): DeckCardResponse[] {
    return deck.cards
      .filter((card) => card.imageUrlSmall)
      .slice(0, 4);
  }

  // ── Delete ─────────────────────────────────────────────

  protected onDeleteDeck(deck: DeckResponse): void {
    if (this.deletingDeckId() !== null) {
      return; // Already deleting
    }

    this.deckPendingDelete.set(deck);
  }

  protected onConfirmDeleteDeck(): void {
    const deck = this.deckPendingDelete();
    if (!deck) {
      return;
    }

    this.deckPendingDelete.set(null);
    this.deletingDeckId.set(deck.id);

    this.deckApi.deleteDeck(deck.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.decks.update((list) => list.filter((d) => d.id !== deck.id));
          this.deletingDeckId.set(null);
        },
        error: () => {
          this.deletingDeckId.set(null);
          this.validationMessages.update((msgs) => ({
            ...msgs,
            [deck.id]: 'No se pudo eliminar el mazo.',
          }));
        },
      });
  }

  protected onCancelDeleteDeck(): void {
    this.deckPendingDelete.set(null);
  }

  // ── Validation ─────────────────────────────────────────

  protected onValidateDeck(deck: DeckResponse): void {
    this.validatingDeckId.set(deck.id);

    this.deckApi.validateDeck(deck.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.decks.update((list) =>
            list.map((d) =>
              d.id === deck.id
                ? { ...d, valid: response.valid, cardCount: response.cardCount, validationErrors: response.errors }
                : d,
            ),
          );
          this.validatingDeckId.set(null);

          if (!response.valid) {
            this.validationMessages.update((msgs) => ({
              ...msgs,
              [deck.id]: 'El mazo todavía no es válido. Revisá los errores.',
            }));
          } else {
            this.validationMessages.update((msgs) => {
              const copy = { ...msgs };
              delete copy[deck.id];
              return copy;
            });
          }
        },
        error: () => {
          this.validatingDeckId.set(null);
          this.validationMessages.update((msgs) => ({
            ...msgs,
            [deck.id]: 'No se pudo validar el mazo.',
          }));
        },
      });
  }
}
