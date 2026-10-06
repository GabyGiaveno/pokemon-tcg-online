import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { ForgotPasswordRequest, PasswordResetResponse, ResetPasswordRequest } from '../models/password-reset.types';

export interface PasswordResetSource {
  forgotPassword(request: ForgotPasswordRequest): Observable<PasswordResetResponse>;
  resetPassword(request: ResetPasswordRequest): Observable<PasswordResetResponse>;
}

export const PASSWORD_RESET_SOURCE = new InjectionToken<PasswordResetSource>('PASSWORD_RESET_SOURCE');
