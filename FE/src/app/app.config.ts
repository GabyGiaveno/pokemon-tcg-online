import { ApplicationConfig, inject, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor';
import { GAME_STATE_SOURCE } from './features/game/services/game-state-source';
import { GameStateMockService } from './features/game/services/game-state-mock.service';
import { GameStateApiService } from './features/game/services/game-state-api.service';
import { GAME_ACTIONS_SOURCE } from './features/game/services/game-actions-source';
import { GameActionsMockService } from './features/game/services/game-actions-mock.service';
import { GameActionsApiService } from './features/game/services/game-actions-api.service';
import { APP_DATA_MODE } from './core/tokens/app-data-mode';
import { AUTH_SOURCE } from './features/auth/data-access/auth-source';
import { AuthMockService } from './features/auth/data-access/auth-mock.service';
import { AuthHttpService } from './features/auth/data-access/auth-http.service';
import { PASSWORD_RESET_SOURCE } from './features/auth/data-access/password-reset-source';
import { PasswordResetMockService } from './features/auth/data-access/password-reset-mock.service';
import { PasswordResetHttpService } from './features/auth/data-access/password-reset-http.service';
import { LOBBY_SOURCE } from './features/auth/data-access/lobby-source';
import { LobbyMockService } from './features/auth/data-access/lobby-mock.service';
import { LobbyHttpService } from './features/auth/data-access/lobby-http.service';
import { WEBSOCKET_SOURCE } from './core/services/websocket-source';
import { WebsocketMockService } from './core/services/websocket-mock.service';
import { WebsocketStompService } from './core/services/websocket-stomp.service';
import { PROFILE_SOURCE } from './features/profile/data-access/profile-source';
import { ProfileMockService } from './features/profile/data-access/profile-mock.service';
import { ProfileHttpService } from './features/profile/data-access/profile-http.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([jwtInterceptor, httpErrorInterceptor])),
    {
      provide: APP_DATA_MODE,
      useValue: 'api',
    },
    {
      provide: AUTH_SOURCE,
      useFactory: () => (inject(APP_DATA_MODE) === 'mock'
        ? inject(AuthMockService)
        : inject(AuthHttpService)),
    },
    {
      provide: PASSWORD_RESET_SOURCE,
      useFactory: () => (inject(APP_DATA_MODE) === 'mock'
        ? inject(PasswordResetMockService)
        : inject(PasswordResetHttpService)),
    },
    {
      provide: LOBBY_SOURCE,
      useFactory: () => (inject(APP_DATA_MODE) === 'mock'
        ? inject(LobbyMockService)
        : inject(LobbyHttpService)),
    },
    {
      provide: GAME_STATE_SOURCE,
      useFactory: () => inject(APP_DATA_MODE) === 'mock'
        ? inject(GameStateMockService)
        : inject(GameStateApiService),
    },
    {
      provide: GAME_ACTIONS_SOURCE,
      useFactory: () => (inject(APP_DATA_MODE) === 'mock'
        ? inject(GameActionsMockService)
        : inject(GameActionsApiService)),
    },
    {
      provide: WEBSOCKET_SOURCE,
      useFactory: () => (inject(APP_DATA_MODE) === 'mock'
        ? inject(WebsocketMockService)
        : inject(WebsocketStompService)),
    },
    {
      provide: PROFILE_SOURCE,
      useFactory: () => (inject(APP_DATA_MODE) === 'mock'
        ? inject(ProfileMockService)
        : inject(ProfileHttpService)),
    },
    provideRouter(routes)
  ]
};
