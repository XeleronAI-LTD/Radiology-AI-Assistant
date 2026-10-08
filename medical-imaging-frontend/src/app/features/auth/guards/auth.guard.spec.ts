import { WritableSignal, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let authService: Partial<AuthService>;
  let isAuthenticated: WritableSignal<boolean>;
  let router: Router;

  function runGuard() {
    return TestBed.runInInjectionContext(() =>
      authGuard(null as never, null as never)
    );
  }

  beforeEach(() => {
    isAuthenticated = signal(false);
    authService = {
      isAuthenticated: isAuthenticated as AuthService['isAuthenticated'],
      clearSession: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: authService }],
    });
    router = TestBed.inject(Router);
  });

  it('laisse passer un utilisateur authentifié', () => {
    isAuthenticated.set(true);

    expect(runGuard()).toBe(true);
    expect(authService.clearSession).not.toHaveBeenCalled();
  });

  it('redirige vers /login et nettoie la session si non authentifié', () => {
    isAuthenticated.set(false);

    const result = runGuard();

    expect(authService.clearSession).toHaveBeenCalled();
    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });
});
