import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs/operators';
import { CardApi } from '../../data-access/card-api.service';
import { DeckApi } from '../../data-access/deck-api.service';
import { DECK_BUILDER_API_PROVIDERS } from '../../data-access/deck-builder-api.provider';
import { GameConfirmDialog } from '../../components/game-confirm-dialog/game-confirm-dialog';
import { CardSearchPanel } from '../../components/card-search-panel/card-search-panel';
import { CardResultList } from '../../components/card-result-list/card-result-list';
import { DeckCurrentList } from '../../components/deck-current-list/deck-current-list';
import { DeckSummary } from '../../components/deck-summary/deck-summary';
import { DeckValidationPanel } from '../../components/deck-validation-panel/deck-validation-panel';
import { CardResponse, CardSearchParams } from '../../models/card.model';
import { DeckBuilderCard } from '../../models/deck-builder-state.model';
import { DeckValidationResponse } from '../../models/deck-validation.model';
import {
  deckCardsToBuilderCards,
  toCreateDeckRequest,
  toUpdateDeckRequest,
} from '../../utils/deck-builder-mappers';
import { SettingsMenu } from '../../../../shared/components/settings-menu/settings-menu';
import { BackgroundMusicService } from '../../../../shared/services/background-music.service';

@Component({
  selector: 'app-deck-builder-page',
  imports: [
    ReactiveFormsModule,
    GameConfirmDialog,
    CardSearchPanel,
    CardResultList,
    DeckCurrentList,
    DeckSummary,
    DeckValidationPanel,
    SettingsMenu,
  ],
  templateUrl: './deck-builder-page.html',
  styleUrl: './deck-builder-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [...DECK_BUILDER_API_PROVIDERS],
})
export class DeckBuilderPage implements OnInit {
  private readonly cardApi = inject(CardApi);
  private readonly deckApi = inject(DeckApi);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly backgroundMusic = inject(BackgroundMusicService);
  private readonly searchInput$ = new Subject<string>();
  private cardRequestId = 0;

  readonly deckForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
  });

  // ── State signals ───────────────────────────────────────────

  readonly cards = signal<CardResponse[]>([]);
  readonly deckCards = signal<DeckBuilderCard[]>([]);
  readonly validationResult = signal<DeckValidationResponse | null>(null);
  readonly loadingCards = signal(false);
  readonly savingDeck = signal(false);
  readonly cardsLoadError = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);
  readonly saveSuccess = signal<string | null>(null);

  /** Non-null when editing an existing deck. */
  readonly editingDeckId = signal<number | null>(null);
  readonly loadingDeck = signal(false);
  readonly loadingDeckError = signal<string | null>(null);
  readonly isEditMode = computed(() => this.editingDeckId() !== null);
  readonly navigateHomePending = signal(false);
  readonly navigateToDecksPending = signal(false);

  readonly searchTerm = signal('');
  readonly currentFilters = signal<CardSearchParams>({});
  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly totalResults = signal(0);

  /** Ticks on each form value change so computed() reacts to Angular form validity. */
  private readonly formChangeTick = signal(0);

  // ── Computed ────────────────────────────────────────────────

  readonly totalCards = computed(() =>
    this.deckCards().reduce((sum, entry) => sum + entry.quantity, 0),
  );

  readonly uniqueCards = computed(() => this.deckCards().length);

  readonly totalPages = computed(() => {
    const size = this.pageSize();
    return size > 0 ? Math.ceil(this.totalResults() / size) : 0;
  });

  readonly displayCurrentPage = computed(() =>
    this.totalPages() === 0 ? 0 : this.currentPage() + 1,
  );

  readonly canGoPrevious = computed(() =>
    this.currentPage() > 0 && !this.loadingCards(),
  );

  readonly canGoNext = computed(() =>
    this.totalPages() > 0
      && this.currentPage() + 1 < this.totalPages()
      && !this.loadingCards(),
  );

  readonly canSave = computed(() => {
    this.formChangeTick(); // create reactive dependency on form value changes
    return this.deckForm.valid
      && this.totalCards() > 0
      && !this.savingDeck()
      && !this.loadingDeck()
      && !this.loadingDeckError();
  });

  // ── Lifecycle ───────────────────────────────────────────────

  constructor() {
    this.deckForm.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.formChangeTick.update((v) => v + 1);
      });

    this.searchInput$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((value) => {
      this.searchTerm.set(value);
      this.currentPage.set(0);
      this.loadCards();
    });
  }

  ngOnInit(): void {
    const deckIdParam = this.route.snapshot.paramMap.get('id');
    if (deckIdParam) {
      this.loadDeckForEdit(Number(deckIdParam));
    }
    this.loadCards();
    this.backgroundMusic.play();
  }

  // ── Card search / filter ────────────────────────────────────

  private loadCards(): void {
    const requestId = ++this.cardRequestId;

    this.loadingCards.set(true);
    this.cardsLoadError.set(null);

    const params = this.buildCardSearchParams();

    this.cardApi.getCards(params).pipe(
      finalize(() => {
        if (requestId === this.cardRequestId) {
          this.loadingCards.set(false);
        }
      }),
    ).subscribe({
      next: (response) => {
        if (requestId !== this.cardRequestId) {
          return;
        }

        this.cards.set(response.data);
        this.totalResults.set(response.total);
        this.currentPage.set(response.page);
        this.pageSize.set(response.size);
      },
      error: () => {
        if (requestId !== this.cardRequestId) {
          return;
        }

        this.cardsLoadError.set('No se pudieron cargar las cartas.');
        this.cards.set([]);
        this.totalResults.set(0);
      },
    });
  }

  /**
   * Loads an existing deck for editing. Populates the form name and
   * deck cards from the API response.
   */
  private loadDeckForEdit(id: number): void {
    this.loadingDeck.set(true);
    this.loadingDeckError.set(null);
    this.editingDeckId.set(id);

    this.deckApi.getDeckById(id).pipe(
      finalize(() => this.loadingDeck.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (deck) => {
        this.deckForm.patchValue({ name: deck.name });
        this.deckCards.set(deckCardsToBuilderCards(deck.cards));
      },
      error: () => {
        this.loadingDeckError.set(
          'No se pudo cargar el mazo para editar. Verificá que exista o volvé a intentar.',
        );
      },
    });
  }

  protected onSearchChange(value: string): void {
    this.searchInput$.next(value.trim());
  }

  protected onFilterChange(params: CardSearchParams): void {
    this.currentFilters.set(params);
    this.currentPage.set(0);
    this.loadCards();
  }

  protected onRetry(): void {
    this.loadCards();
  }

  protected onPreviousPage(): void {
    if (!this.canGoPrevious()) {
      return;
    }

    this.currentPage.update((page) => page - 1);
    this.loadCards();
  }

  protected onNextPage(): void {
    if (!this.canGoNext()) {
      return;
    }

    this.currentPage.update((page) => page + 1);
    this.loadCards();
  }

  // ── Deck card management ────────────────────────────────────

  protected onAddCard(card: CardResponse): void {
    this.clearMessages();
    this.deckCards.update((current) => {
      const existing = current.find((dc) => dc.card.id === card.id);
      if (existing) {
        return current.map((dc) =>
          dc.card.id === card.id
            ? { ...dc, quantity: dc.quantity + 1 }
            : dc,
        );
      }
      return [...current, { card, quantity: 1 }];
    });
  }

  protected onIncreaseQuantity(cardId: string): void {
    this.clearMessages();
    this.deckCards.update((current) =>
      current.map((dc) =>
        dc.card.id === cardId
          ? { ...dc, quantity: dc.quantity + 1 }
          : dc,
      ),
    );
  }

  protected onDecreaseQuantity(cardId: string): void {
    this.clearMessages();
    this.deckCards.update((current) => {
      const entry = current.find((dc) => dc.card.id === cardId);
      if (!entry) {
        return current;
      }
      if (entry.quantity <= 1) {
        return current.filter((dc) => dc.card.id !== cardId);
      }
      return current.map((dc) =>
        dc.card.id === cardId
          ? { ...dc, quantity: dc.quantity - 1 }
          : dc,
      );
    });
  }

  protected onRemoveCard(cardId: string): void {
    this.clearMessages();
    this.deckCards.update((current) =>
      current.filter((dc) => dc.card.id !== cardId),
    );
  }

  protected onClearDeck(): void {
    this.deckCards.set([]);
    this.clearMessages();
  }

  // ── Navigation ─────────────────────────────────────────────

  protected onNavigateHome(): void {
    this.navigateHomePending.set(true);
  }

  protected onConfirmNavigateHome(): void {
    this.navigateHomePending.set(false);
    void this.router.navigate(['/lobby']);
  }

  protected onCancelNavigateHome(): void {
    this.navigateHomePending.set(false);
  }

  protected onNavigateToDecks(): void {
    this.navigateToDecksPending.set(true);
  }

  protected onConfirmNavigateToDecks(): void {
    this.navigateToDecksPending.set(false);
    void this.router.navigate(['/decks']);
  }

  protected onCancelNavigateToDecks(): void {
    this.navigateToDecksPending.set(false);
  }

  // ── Save ────────────────────────────────────────────────────

  protected onSave(): void {
    this.saveError.set(null);
    this.saveSuccess.set(null);
    this.validationResult.set(null);

    // Touch all controls so validation messages in template appear
    Object.keys(this.deckForm.controls).forEach((key) => {
      this.deckForm.get(key)?.markAsTouched();
    });

    if (this.deckForm.invalid) {
      this.saveError.set(
        'El nombre del mazo es obligatorio (máx. 100 caracteres).',
      );
      return;
    }

    const currentDeck = this.deckCards();
    if (currentDeck.length === 0) {
      this.saveError.set(
        'Agregá al menos una carta al mazo antes de guardar.',
      );
      return;
    }

    if (currentDeck.some((entry) => entry.quantity <= 0)) {
      this.saveError.set('Todas las cartas del mazo deben tener una cantidad positiva.');
      return;
    }

    const name = this.deckForm.getRawValue().name;
    this.savingDeck.set(true);

    const editId = this.editingDeckId();
    const save$ = editId !== null
      ? this.deckApi.updateDeck(editId, toUpdateDeckRequest(name, currentDeck))
      : this.deckApi.createDeck(toCreateDeckRequest(name, currentDeck));

    save$.pipe(
      finalize(() => this.savingDeck.set(false)),
    ).subscribe({
      next: (deck) => {
        const prefix = editId !== null ? 'Mazo actualizado' : 'Mazo guardado';
        const suffix = deck.valid
          ? 'Está válido y listo para jugar.'
          : 'Todavía no es válido. Revisá los errores de validación.';
        this.saveSuccess.set(`${prefix} correctamente. ${suffix} ID: ${deck.id}.`);
        this.validationResult.set({
          valid: deck.valid,
          errors: deck.validationErrors ?? [],
          cardCount: deck.cardCount,
        });
      },
      error: (err: HttpErrorResponse) => {
        this.saveError.set(this.getSaveErrorMessage(err));
      },
    });
  }

  // ── Internal helpers ────────────────────────────────────────

  private clearMessages(): void {
    this.saveSuccess.set(null);
    this.saveError.set(null);
    this.validationResult.set(null);
  }

  private buildCardSearchParams(): CardSearchParams {
    const params: CardSearchParams = {
      ...this.currentFilters(),
      set: 'xy1',
      page: this.currentPage(),
      size: this.pageSize(),
    };

    const term = this.searchTerm();
    if (term) {
      params.name = term;
    }

    return params;
  }

  private getSaveErrorMessage(err: HttpErrorResponse): string {
    if (err.status === 401 || err.status === 403) {
      return 'Tu sesión no es válida o expiró. Volvé a iniciar sesión.';
    }

    if (err.status === 409) {
      return 'No se pudo guardar el mazo por un conflicto de datos. ' +
        'Intentá de nuevo.';
    }

    // If the backend sent a safe message, use it
    if (err.error?.message && typeof err.error.message === 'string') {
      return err.error.message;
    }

    return 'No se pudo guardar el mazo.';
  }
}
