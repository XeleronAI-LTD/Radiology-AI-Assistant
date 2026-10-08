import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PageResponse } from '../../../../shared/models/page-response.model';
import { AuthService } from '../../../auth/services/auth.service';
import { CurrentUser } from '../../../auth/models/auth.model';
import { ExamenSummary } from '../../models/examen.model';
import { ExamenService } from '../../services/examen.service';
import { ExamensListe } from './examens-liste';

function pageOf(examens: ExamenSummary[], page = 0, totalPages = 1): PageResponse<ExamenSummary> {
  return { content: examens, page, size: 20, totalElements: examens.length, totalPages };
}

const EXAMEN: ExamenSummary = {
  examenId: 1,
  mrn: 'MRN-001',
  patientNom: 'Dupont',
  type: 'Radio thorax',
  dateExamen: '2026-08-20',
  modalite: 'CR',
  zone: 'THORAX',
  nombreImages: 2,
};

describe('ExamensListe', () => {
  let examenService: Partial<ExamenService>;
  let authService: Partial<AuthService>;
  let currentUser: ReturnType<typeof signal<CurrentUser | null>>;
  let router: Router;

  beforeEach(async () => {
    examenService = { lister: vi.fn().mockReturnValue(of(pageOf([]))) };
    currentUser = signal<CurrentUser | null>(null);
    authService = { currentUser: currentUser as AuthService['currentUser'] };

    await TestBed.configureTestingModule({
      imports: [ExamensListe],
      providers: [
        provideRouter([]),
        { provide: ExamenService, useValue: examenService },
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();
    router = TestBed.inject(Router);
  });

  it('charge la première page au démarrage', () => {
    TestBed.createComponent(ExamensListe).detectChanges();

    expect(examenService.lister).toHaveBeenCalledWith(null, 0, 20);
  });

  it('affiche le tableau après un chargement réussi', () => {
    (examenService.lister as ReturnType<typeof vi.fn>).mockReturnValue(of(pageOf([EXAMEN])));

    const fixture = TestBed.createComponent(ExamensListe);
    fixture.detectChanges();

    expect(fixture.componentInstance['examens']()).toEqual([EXAMEN]);
    expect(fixture.componentInstance['loading']()).toBe(false);
  });

  it("affiche l'état vide quand la liste est vide", () => {
    const fixture = TestBed.createComponent(ExamensListe);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Aucun examen trouvé');
  });

  it("affiche l'erreur et propose de réessayer", () => {
    (examenService.lister as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('boom'))
    );

    const fixture = TestBed.createComponent(ExamensListe);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component['error']()).toBe('Impossible de charger les examens, réessayez plus tard.');

    (examenService.lister as ReturnType<typeof vi.fn>).mockReturnValue(of(pageOf([EXAMEN])));
    component['onRetry']();

    expect(examenService.lister).toHaveBeenLastCalledWith(null, 0, 20);
    expect(component['error']()).toBeNull();
  });

  it('la recherche MRN trim la valeur et réinitialise la page à 0', () => {
    const fixture = TestBed.createComponent(ExamensListe);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component['onSearch']('  MRN-001  ');

    expect(examenService.lister).toHaveBeenLastCalledWith('MRN-001', 0, 20);
  });

  it("soumettre le formulaire de recherche (submit natif du DOM) déclenche onSearch et empêche le rechargement de page", () => {
    const fixture = TestBed.createComponent(ExamensListe);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input[type="text"]') as HTMLInputElement;
    input.value = 'MRN-001';
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    const submitEvent = new Event('submit', { cancelable: true });
    form.dispatchEvent(submitEvent);

    expect(examenService.lister).toHaveBeenLastCalledWith('MRN-001', 0, 20);
    expect(submitEvent.defaultPrevented).toBe(true);
  });

  it('onNext/onPrevious respectent les bornes de pagination', () => {
    (examenService.lister as ReturnType<typeof vi.fn>).mockReturnValue(
      of(pageOf([EXAMEN], 0, 3))
    );

    const fixture = TestBed.createComponent(ExamensListe);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component['onPrevious'](); // déjà en page 0, ne doit rien déclencher de plus
    expect(examenService.lister).toHaveBeenCalledTimes(1);

    component['onNext']();
    expect(examenService.lister).toHaveBeenLastCalledWith(null, 1, 20);
  });

  it('un clic sur une ligne navigue vers /examens/:id', () => {
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    (examenService.lister as ReturnType<typeof vi.fn>).mockReturnValue(of(pageOf([EXAMEN])));

    const fixture = TestBed.createComponent(ExamensListe);
    fixture.detectChanges();
    fixture.componentInstance['onRowClick'](EXAMEN.examenId);

    expect(navigateSpy).toHaveBeenCalledWith(['/examens', EXAMEN.examenId]);
  });

  it('affiche le bouton "+ Nouvel examen" pour un RADIOLOGUE', () => {
    currentUser.set({ email: 'radio@test.com', role: 'RADIOLOGUE', exp: 9999999999 });

    const fixture = TestBed.createComponent(ExamensListe);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('+ Nouvel examen');
  });

  it('masque le bouton "+ Nouvel examen" pour un ADMIN', () => {
    currentUser.set({ email: 'admin@test.com', role: 'ADMIN', exp: 9999999999 });

    const fixture = TestBed.createComponent(ExamensListe);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('+ Nouvel examen');
  });

  it('le clic sur "+ Nouvel examen" navigue vers /examens/nouveau', () => {
    currentUser.set({ email: 'radio@test.com', role: 'RADIOLOGUE', exp: 9999999999 });
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const fixture = TestBed.createComponent(ExamensListe);
    fixture.detectChanges();
    fixture.componentInstance['onNouvelExamen']();

    expect(navigateSpy).toHaveBeenCalledWith(['/examens/nouveau']);
  });
});
