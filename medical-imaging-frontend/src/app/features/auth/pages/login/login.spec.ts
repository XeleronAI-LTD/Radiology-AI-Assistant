import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Login } from './login';

describe('Login', () => {
  let authService: Partial<AuthService>;

  beforeEach(async () => {
    authService = { login: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    }).compileComponents();
  });

  it('désactive le bouton tant que le formulaire est invalide', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(button.disabled).toBe(true);
  });

  it('appelle authService.login() avec les valeurs du formulaire à la soumission', () => {
    (authService.login as ReturnType<typeof vi.fn>).mockReturnValue(of({ token: 'x' }));

    const fixture = TestBed.createComponent(Login);
    const component = fixture.componentInstance;
    component['form'].setValue({ email: 'user@xeleronai.com', password: 'secret' });
    fixture.detectChanges();

    component['onSubmit']();

    expect(authService.login).toHaveBeenCalledWith({
      email: 'user@xeleronai.com',
      password: 'secret',
    });
  });

  it('affiche un message générique sur une erreur 401', () => {
    (authService.login as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401 }))
    );

    const fixture = TestBed.createComponent(Login);
    const component = fixture.componentInstance;
    component['form'].setValue({ email: 'user@xeleronai.com', password: 'secret' });
    fixture.detectChanges();

    component['onSubmit']();
    fixture.detectChanges();

    expect(component['errorMessage']()).toBe('Email ou mot de passe incorrect');
  });

  it('affiche le message de rate-limiting sur une erreur 429 et garde le mot de passe', () => {
    (authService.login as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 429 }))
    );

    const fixture = TestBed.createComponent(Login);
    const component = fixture.componentInstance;
    component['form'].setValue({ email: 'user@xeleronai.com', password: 'secret' });
    fixture.detectChanges();

    component['onSubmit']();
    fixture.detectChanges();

    expect(component['errorMessage']()).toBe('Trop de tentatives, réessayez dans une minute.');
    expect(component['form'].getRawValue().password).toBe('secret');
  });
});
