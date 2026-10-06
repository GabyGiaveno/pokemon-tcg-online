import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LoginRequest, PlayerResponse, RegisterRequest } from '../models/auth-api.types';
import { AuthSource } from './auth-source';

@Injectable({ providedIn: 'root' })
export class AuthHttpService implements AuthSource {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/auth';

  login(request: LoginRequest): Observable<PlayerResponse> {
    return this.http.post<PlayerResponse>(`${this.baseUrl}/login`, request);
  }

  register(request: RegisterRequest): Observable<PlayerResponse> {
    return this.http.post<PlayerResponse>(`${this.baseUrl}/register`, request);
  }
}
