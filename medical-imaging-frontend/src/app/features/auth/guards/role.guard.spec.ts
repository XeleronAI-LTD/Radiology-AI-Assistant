import { WritableSignal, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { CurrentUser } from '../models/auth.model';
import { AuthService } from '../services/auth.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  let authService: Partial<AuthService>;
  let currentUser: WritableSignal<CurrentUser | null>;
  let router: Router;

  function runGuard(roles: string[] | undefined) {
    const route = { data: roles ? { roles } : {} } as unknown as ActivatedRouteSnapshot;
    return TestBed.runInInjectionContext(() => roleGuard(route, null as never));
  }

  beforeEach(() => {
    currentUser = signal<CurrentUser | null>(null);
    authService = {
      currentUser: currentUser as AuthService['currentUser'],
    };

    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: authService }],
    });
    router = TestBed.inject(Router);
  });

  it('laisse passer un rôle présent dans route.data.roles', () => {
    currentUser.set({ email: 'radio@test.com', role: 'RADIOLOGUE', exp: 9999999999 });

    expect(runGuard(['RADIOLOGUE', 'TECHNICIEN'])).toBe(true);
  });

  it('redirige vers / un rôle absent de route.data.roles', () => {
    currentUser.set({ email: 'admin@test.com', role: 'ADMIN', exp: 9999999999 });

    const result = runGuard(['RADIOLOGUE', 'TECHNICIEN']);

    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toBe('/');
  });

  it("redirige vers / quand aucun utilisateur courant n'est présent", () => {
    currentUser.set(null);

    const result = runGuard(['RADIOLOGUE', 'TECHNICIEN']);

    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toBe('/');
  });

  it('laisse passer quand la route ne définit aucun rôle requis', () => {
    currentUser.set(null);

    expect(runGuard(undefined)).toBe(true);
  });
});
