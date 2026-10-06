import { Provider } from '@angular/core';
import { CardApi } from './card-api.service';
import { DeckApi } from './deck-api.service';
import { RealCardApiService } from './real-card-api.service';
import { RealDeckApiService } from './real-deck-api.service';

/**
 * Provider array that wires the abstract {@link CardApi} and {@link DeckApi}
 * to concrete implementations.
 *
 * ── Switching to mocks ──────────────────────────────────────────
 * Replace `RealCardApiService` / `RealDeckApiService` with:
 *   - `MockCardApiService`  (from `./mock-card-api.service`)
 *   - `MockDeckApiService`  (from `./mock-deck-api.service`)
 *
 * Example:
 * ```typescript
 * import { MockCardApiService } from './mock-card-api.service';
 * import { MockDeckApiService } from './mock-deck-api.service';
 *
 * export const DECK_BUILDER_API_PROVIDERS: Provider[] = [
 *   { provide: CardApi, useClass: MockCardApiService },
 *   { provide: DeckApi, useClass: MockDeckApiService },
 * ];
 * ```
 */
export const DECK_BUILDER_API_PROVIDERS: Provider[] = [
  {
    provide: CardApi,
    useClass: RealCardApiService,
  },
  {
    provide: DeckApi,
    useClass: RealDeckApiService,
  },
];
