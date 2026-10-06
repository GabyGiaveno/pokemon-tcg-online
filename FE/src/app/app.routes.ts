import { Routes } from '@angular/router';
import { AUTH_ROUTES } from './features/auth/auth.routes';
import { PokedexPage } from './features/pokedex/pages/pokedex-page/pokedex-page';
import { LobbyPage } from './features/auth/lobby/lobby';
import { authGuard } from './core/guards/auth.guard';


export const routes: Routes = [
  {
    path: '',
    redirectTo: 'auth/login',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    children: AUTH_ROUTES
  },
  {
    path: 'pokedex',
    component: PokedexPage
  },
  {
    path: 'lobby',
    component: LobbyPage,
    canActivate: [authGuard],
  },
  {
    path: 'perfil',
    loadComponent: () =>
      import('./features/profile/pages/profile-page/profile-page')
        .then(m => m.ProfilePage),
    canActivate: [authGuard],
  },
  {
    path: 'game',
    loadChildren: () => import('./features/game/game.routes').then((m) => m.GAME_ROUTES)
  },
  {
    path: 'decks/new',
    loadComponent: () =>
      import('./features/deck-builder/pages/deck-builder-page/deck-builder-page')
        .then(m => m.DeckBuilderPage),
    canActivate: [authGuard],
  },
  {
    path: 'decks/:id/edit',
    loadComponent: () =>
      import('./features/deck-builder/pages/deck-builder-page/deck-builder-page')
        .then(m => m.DeckBuilderPage),
    canActivate: [authGuard],
  },
  {
    path: 'decks',
    pathMatch: 'full',
    loadComponent: () =>
      import('./features/deck-builder/pages/deck-list-page/deck-list-page')
        .then(m => m.DeckListPage),
    canActivate: [authGuard],
  },
  {
    path: 'reglas',
    loadComponent: () =>
      import('./features/rules/pages/rules-page/rules-page')
        .then(m => m.RulesPage),
  },
  {
    path: '**',
    loadComponent: () =>
      import('./features/auth/not-found/not-found')
        .then(m => m.NotFoundPage)
  }
];
