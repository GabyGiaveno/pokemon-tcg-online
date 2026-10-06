import { Injectable, signal } from '@angular/core';

const TOKEN_KEY = 'pokemon_tcg_token';
const USERNAME_KEY = 'pokemon_tcg_username';
const PLAYER_ID_KEY = 'pokemon_tcg_player_id';

@Injectable({ providedIn: 'root' })
export class AuthTokenService {
  private readonly token = signal<string | null>(this.loadToken());
  private readonly username = signal<string | null>(this.loadUsername());
  private readonly playerId = signal<number | null>(this.loadPlayerId());

  set(token: string): void {
    this.token.set(token);
    localStorage.setItem(TOKEN_KEY, token);
  }

  setSession(token: string, username?: string, playerId?: number): void {
    this.set(token);

    if (username !== undefined) {
      this.username.set(username);
      localStorage.setItem(USERNAME_KEY, username);
    }

    if (playerId !== undefined) {
      this.playerId.set(playerId);
      localStorage.setItem(PLAYER_ID_KEY, String(playerId));
    }
  }

  clear(): void {
    this.token.set(null);
    this.username.set(null);
    this.playerId.set(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(PLAYER_ID_KEY);
  }

  get value(): string | null {
    return this.token();
  }

  get usernameValue(): string | null {
    return this.username();
  }

  get playerIdValue(): number | null {
    return this.playerId();
  }

  private loadToken(): string | null {
    return this.readLocalStorage(TOKEN_KEY);
  }

  private loadUsername(): string | null {
    return this.readLocalStorage(USERNAME_KEY);
  }

  private loadPlayerId(): number | null {
    const value = this.readLocalStorage(PLAYER_ID_KEY);
    if (!value) {
      return null;
    }

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private readLocalStorage(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }
}
