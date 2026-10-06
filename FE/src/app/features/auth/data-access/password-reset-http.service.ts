import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ForgotPasswordRequest, PasswordResetResponse, ResetPasswordRequest } from '../models/password-reset.types';
import { PasswordResetSource } from './password-reset-source';

@Injectable({ providedIn: 'root' })
export class PasswordResetHttpService implements PasswordResetSource {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/auth';

  forgotPassword(request: ForgotPasswordRequest): Observable<PasswordResetResponse> {
    return this.http.post<PasswordResetResponse>(`${this.baseUrl}/forgot-password`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<PasswordResetResponse> {
    return this.http.post<PasswordResetResponse>(`${this.baseUrl}/reset-password`, request);
  }
}
