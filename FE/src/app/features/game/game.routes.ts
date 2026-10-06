import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const GAME_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./board-container/board-container').then((m) => m.BoardContainer),
    canActivate: [authGuard],
  },
  {
    path: ':gameId',
    loadComponent: () => import('./board-container/board-container').then((m) => m.BoardContainer),
    canActivate: [authGuard],
  }
];
