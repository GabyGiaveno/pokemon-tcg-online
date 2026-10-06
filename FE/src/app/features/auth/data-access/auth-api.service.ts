import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { LoginRequest, PlayerResponse, RegisterRequest } from '../models/auth-api.types';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/auth';

  login(request: LoginRequest) {
    return this.http.post<PlayerResponse>(`${this.baseUrl}/login`, request);
  }

  register(request: RegisterRequest) {
    return this.http.post<PlayerResponse>(`${this.baseUrl}/register`, request);
  }
}
