import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { LoginRequest, PlayerResponse, RegisterRequest } from '../models/auth-api.types';
import { loginMockUser, registerMockUser } from './mock-auth-store';
import { AuthSource } from './auth-source';

@Injectable({ providedIn: 'root' })
export class AuthMockService implements AuthSource {
  login(request: LoginRequest): Observable<PlayerResponse> {
    try {
      return of(loginMockUser(request));
    } catch (error) {
      return throwError(() => error instanceof Error ? error : new Error('Invalid credentials'));
    }
  }

  register(request: RegisterRequest): Observable<PlayerResponse> {
    try {
      return of(registerMockUser(request));
    } catch (error) {
      return throwError(() => error instanceof Error ? error : new Error('Registration failed'));
    }
  }
}
