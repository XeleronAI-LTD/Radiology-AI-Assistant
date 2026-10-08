import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../features/auth/services/auth.service';
import { ProtectedLayout } from './protected-layout';

describe('ProtectedLayout', () => {
  let authService: Partial<AuthService>;

  beforeEach(async () => {
    authService = { logout: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ProtectedLayout],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    }).compileComponents();
  });

  it('appelle authService.logout() au clic sur Déconnexion', () => {
    const fixture = TestBed.createComponent(ProtectedLayout);
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    button.click();

    expect(authService.logout).toHaveBeenCalled();
  });
});
