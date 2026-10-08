import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { AuthService } from './auth.service';

function fakeJwt(payload: Record<string, unknown>): string {
  const base64url = (obj: unknown) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64url({ alg: 'HS256' })}.${base64url(payload)}.signature`;
}

describe('AuthService', () => {
  let httpMock: HttpTestingController;
  let router: Router;
  const TOKEN_KEY = 'auth_token';

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('stocke le token et met à jour currentUser après un login réussi', () => {
    const service = TestBed.inject(AuthService);
    const futureExp = Math.floor(Date.now() / 1000) + 3600;
    const token = fakeJwt({ sub: 'radiologue@xeleronai.com', role: 'RADIOLOGUE', exp: futureExp });

    service.login({ email: 'radiologue@xeleronai.com', password: 'secret' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush({ token });

    expect(localStorage.getItem(TOKEN_KEY)).toBe(token);
    expect(service.currentUser()).toEqual({
      email: 'radiologue@xeleronai.com',
      role: 'RADIOLOGUE',
      exp: futureExp,
    });
    expect(service.isAuthenticated()).toBe(true);
  });

  it("ne stocke rien si le login échoue (401)", () => {
    const service = TestBed.inject(AuthService);

    service.login({ email: 'x@x.com', password: 'wrong' }).subscribe({
      error: () => {},
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/auth/login`);
    req.flush('Invalid credentials', { status: 401, statusText: 'Unauthorized' });

    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(service.currentUser()).toBeNull();
  });

  it('restaure la session depuis un token valide déjà en localStorage', () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600;
    const token = fakeJwt({ sub: 'admin@xeleronai.com', role: 'ADMIN', exp: futureExp });
    localStorage.setItem(TOKEN_KEY, token);

    const service = TestBed.inject(AuthService);

    expect(service.currentUser()?.email).toBe('admin@xeleronai.com');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('nettoie un token corrompu présent en localStorage', () => {
    localStorage.setItem(TOKEN_KEY, 'not-a-valid-jwt');

    const service = TestBed.inject(AuthService);

    expect(service.currentUser()).toBeNull();
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('isAuthenticated() est false si le token est expiré', () => {
    const pastExp = Math.floor(Date.now() / 1000) - 60;
    const token = fakeJwt({ sub: 'x@x.com', role: 'TECHNICIEN', exp: pastExp });
    localStorage.setItem(TOKEN_KEY, token);

    const service = TestBed.inject(AuthService);

    expect(service.isAuthenticated()).toBe(false);
  });

  it('logout() vide la session et redirige vers /login', () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600;
    localStorage.setItem(TOKEN_KEY, fakeJwt({ sub: 'x@x.com', role: 'ADMIN', exp: futureExp }));
    const service = TestBed.inject(AuthService);

    service.logout();

    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
