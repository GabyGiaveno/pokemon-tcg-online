import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthTokenService } from '../../features/auth/data-access/auth-token.service';
import { catchError, throwError } from 'rxjs';

/**
 * Intercepts HTTP errors globally:
 * - 401 Unauthorized → clears token, redirects to /auth/login
 * - 403 / 5xx → logs to console (future: show toast)
 */
export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authToken = inject(AuthTokenService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        authToken.clear();
        router.navigate(['/auth/login']);
      } else if (error.status >= 500) {
        const serverMsg = error.error?.message ?? error.error?.error ?? '(sin detalle)';
        console.error(`[HTTP ${error.status}] Server error: ${serverMsg}`, error.message);
      }
      return throwError(() => error);
    }),
  );
};
