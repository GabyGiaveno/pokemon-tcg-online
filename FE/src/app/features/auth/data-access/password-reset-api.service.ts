import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ForgotPasswordRequest,
  PasswordResetResponse,
  ResetPasswordRequest,
} from '../models/password-reset.types';
import { PASSWORD_RESET_SOURCE } from './password-reset-source';

@Injectable({ providedIn: 'root' })
export class PasswordResetApiService {
  private readonly source = inject(PASSWORD_RESET_SOURCE);

  forgotPassword(request: ForgotPasswordRequest): Observable<PasswordResetResponse> {
    return this.source.forgotPassword(request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<PasswordResetResponse> {
    return this.source.resetPassword(request);
  }
}
