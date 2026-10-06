import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { LoginRequest, PlayerResponse, RegisterRequest } from '../models/auth-api.types';

export interface AuthSource {
  login(request: LoginRequest): Observable<PlayerResponse>;
  register(request: RegisterRequest): Observable<PlayerResponse>;
}

export const AUTH_SOURCE = new InjectionToken<AuthSource>('AUTH_SOURCE');
