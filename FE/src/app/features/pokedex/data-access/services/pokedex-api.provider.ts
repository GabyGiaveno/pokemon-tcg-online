import { Provider, inject } from '@angular/core';
import { APP_DATA_MODE } from '../../../../core/tokens/app-data-mode';
import { PokedexApi } from './pokedex-api.service';
import { MockPokedexApiService } from './mock-pokedex-api.service';
import { HttpPokedexApiService } from './http-pokedex-api.service';

export const POKEDEX_API_PROVIDERS: Provider[] = [
  {
    provide: PokedexApi,
    useFactory: () => inject(APP_DATA_MODE) === 'mock'
      ? inject(MockPokedexApiService)
      : inject(HttpPokedexApiService),
  },
];
