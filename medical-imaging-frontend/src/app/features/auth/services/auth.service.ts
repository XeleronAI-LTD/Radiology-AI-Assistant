import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthResponse, CurrentUser, LoginRequest } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly TOKEN_KEY = 'auth_token';

  readonly currentUser = signal<CurrentUser | null>(this.readStoredUser());
  readonly isAuthenticated = computed(() => {
    const user = this.currentUser();
    return user !== null && user.exp * 1000 > Date.now();
  });

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/v1/auth/login`, credentials)
      .pipe(
        tap((response) => {
          localStorage.setItem(this.TOKEN_KEY, response.token);
          this.currentUser.set(this.decodeToken(response.token));
        })
      );
  }

  logout(): void {
    this.clearSession();
    this.router.navigateByUrl('/login');
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /** Nettoie la session sans naviguer — utilisé par logout() et par authGuard,
   *  qui gèrent chacun leur propre redirection pour éviter une double navigation. */
  clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.currentUser.set(null);
  }

  private readStoredUser(): CurrentUser | null {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (!token) {
      return null;
    }
    const user = this.decodeToken(token);
    if (!user) {
      localStorage.removeItem(this.TOKEN_KEY);
    }
    return user;
  }

  private decodeToken(token: string): CurrentUser | null {
    try {
      const payloadSegment = token.split('.')[1];
      const base64 = payloadSegment.replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(base64));
      return { email: payload.sub, role: payload.role, exp: payload.exp };
    } catch {
      return null;
    }
  }
}
