import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthTokenService } from '../../features/auth/data-access/auth-token.service';

/**
 * Prevents navigation to authenticated routes when no valid JWT token is present.
 * Reads the token from {@link AuthTokenService} (persisted in localStorage).
 * Redirects to `/auth/login` when no token is found.
 */
export const authGuard: CanActivateFn = () => {
  const authToken = inject(AuthTokenService);
  const router = inject(Router);

  if (authToken.value) {
    return true;
  }

  return router.parseUrl('/auth/login');
};
