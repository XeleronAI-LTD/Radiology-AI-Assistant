import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let http: HttpClient;
  let authService: Partial<AuthService>;

  beforeEach(() => {
    authService = {
      getToken: vi.fn().mockReturnValue('fake-token'),
      logout: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it("ajoute le header Authorization sur une requête vers l'API avec un token présent", () => {
    http.get(`${environment.apiUrl}/v1/examens`).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/examens`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer fake-token');
    req.flush({});
  });

  it("n'ajoute pas le header Authorization sur une requête hors API", () => {
    http.get('https://autre-domaine.example.com/data').subscribe();

    const req = httpMock.expectOne('https://autre-domaine.example.com/data');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('déconnecte sur un 401 reçu en dehors du login', () => {
    http.get(`${environment.apiUrl}/v1/examens`).subscribe({ error: () => {} });

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/examens`);
    req.flush('unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authService.logout).toHaveBeenCalled();
  });

  it("ne déconnecte pas sur un 401 reçu depuis l'endpoint de login", () => {
    http.post(`${environment.apiUrl}/v1/auth/login`, {}).subscribe({ error: () => {} });

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/auth/login`);
    req.flush('invalid credentials', { status: 401, statusText: 'Unauthorized' });

    expect(authService.logout).not.toHaveBeenCalled();
  });
});
