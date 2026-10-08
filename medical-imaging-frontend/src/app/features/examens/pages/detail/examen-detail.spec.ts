import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../auth/services/auth.service';
import { AnalyseStatutResponse, Detection } from '../../models/detection.model';
import { ExamenDetail as ExamenDetailModel } from '../../models/examen.model';
import { DetectionMasqueStore } from '../../services/detection-masque-store.service';
import { DetectionService } from '../../services/detection.service';
import { ExamenImageApercuStore } from '../../services/examen-image-apercu-store.service';
import { ExamenService } from '../../services/examen.service';
import { ExamenDetail } from './examen-detail';

const EXAMEN: ExamenDetailModel = {
  examenId: 1,
  patient: { patientId: 1, mrn: 'MRN-001', nom: 'Dupont', dateNaissance: '1980-01-01', sexe: 'M' },
  type: 'Radio thorax',
  dateExamen: '2026-08-20',
  modalite: 'CR',
  zone: 'THORAX',
  creePar: 'radiologue@xeleronai.com',
  images: [{ imageId: 1, format: 'PNG', apercuDisponible: true, ordre: 0 }],
};

const DETECTION: Detection = {
  id: 1,
  imageId: 1,
  type: 'BOX',
  anomalie: 'nodule',
  confiance: 0.9,
  statut: 'EN_ATTENTE',
  coupe: 0,
  bbox: { x: 1, y: 2, largeur: 3, hauteur: 4 },
  apercuMasqueDisponible: false,
  validateurEmail: null,
  valideLe: null,
};

const STATUT_EN_ATTENTE: AnalyseStatutResponse = {
  examenId: 1,
  statut: 'EN_ATTENTE',
  message: null,
  finieLe: null,
};

function configure(
  id: string,
  examenService: Partial<ExamenService>,
  apercuStore: Partial<ExamenImageApercuStore>,
  detectionService: Partial<DetectionService>,
  detectionMasqueStore: Partial<DetectionMasqueStore>,
  authService: Partial<AuthService>
) {
  return TestBed.configureTestingModule({
    imports: [ExamenDetail],
    providers: [
      provideRouter([]),
      { provide: ExamenService, useValue: examenService },
      { provide: ExamenImageApercuStore, useValue: apercuStore },
      { provide: DetectionService, useValue: detectionService },
      { provide: DetectionMasqueStore, useValue: detectionMasqueStore },
      { provide: AuthService, useValue: authService },
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id }) } } },
    ],
  }).compileComponents();
}

describe('ExamenDetail', () => {
  let examenService: Partial<ExamenService>;
  let apercuStore: Partial<ExamenImageApercuStore>;
  let detectionService: Partial<DetectionService>;
  let detectionMasqueStore: Partial<DetectionMasqueStore>;
  let authService: Partial<AuthService>;

  beforeEach(() => {
    examenService = {
      detail: vi.fn().mockReturnValue(of(EXAMEN)),
      apercu: vi.fn().mockReturnValue(of(new Blob(['x']))),
    };
    apercuStore = {
      charger: vi.fn(),
      entree: vi.fn().mockReturnValue(signal({ objectUrl: 'blob:fake-url', loading: false, error: false })),
      clearAll: vi.fn(),
    };
    detectionService = {
      lister: vi.fn().mockReturnValue(of([])),
      statutAnalyse: vi.fn().mockReturnValue(of(STATUT_EN_ATTENTE)),
      lancerAnalyse: vi.fn().mockReturnValue(of({ examenId: 1, statut: 'EN_COURS' })),
      validerStatut: vi.fn().mockReturnValue(of({ ...DETECTION, statut: 'ACCEPTEE', validateurEmail: 'radio@test.com' })),
    };
    detectionMasqueStore = {
      clearAll: vi.fn(),
    };
    authService = {
      currentUser: signal({ email: 'radio@test.com', role: 'RADIOLOGUE', exp: 9999999999 }),
    };
  });

  function createFixture(id = '1') {
    return configure(
      id,
      examenService,
      apercuStore,
      detectionService,
      detectionMasqueStore,
      authService
    ).then(() => TestBed.createComponent(ExamenDetail));
  }

  it('charge le détail avec l\'id numérique lu depuis la route', async () => {
    await configure('1', examenService, apercuStore, detectionService, detectionMasqueStore, authService);
    TestBed.createComponent(ExamenDetail).detectChanges();

    expect(examenService.detail).toHaveBeenCalledWith(1);
  });

  it('affiche les infos patient/examen et une tuile par image', async () => {
    await configure('1', examenService, apercuStore, detectionService, detectionMasqueStore, authService);
    const fixture = TestBed.createComponent(ExamenDetail);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('MRN-001');
    expect(text).toContain('Radio thorax');
    expect(fixture.nativeElement.querySelectorAll('app-examen-image-apercu').length).toBe(1);
  });

  it('affiche un message quand l\'examen n\'a aucune image', async () => {
    (examenService.detail as ReturnType<typeof vi.fn>).mockReturnValue(of({ ...EXAMEN, images: [] }));
    await configure('1', examenService, apercuStore, detectionService, detectionMasqueStore, authService);
    const fixture = TestBed.createComponent(ExamenDetail);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucune image pour cet examen.');
  });

  it('affiche "Examen introuvable" sans bouton Réessayer sur un 404', async () => {
    (examenService.detail as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 }))
    );
    await configure('1', examenService, apercuStore, detectionService, detectionMasqueStore, authService);
    const fixture = TestBed.createComponent(ExamenDetail);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Examen introuvable');
    expect(text).not.toContain('Réessayer');
  });

  it('affiche une erreur générique avec bouton Réessayer sur un 500', async () => {
    (examenService.detail as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );
    await configure('1', examenService, apercuStore, detectionService, detectionMasqueStore, authService);
    const fixture = TestBed.createComponent(ExamenDetail);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain("Impossible de charger l'examen");

    (examenService.detail as ReturnType<typeof vi.fn>).mockReturnValue(of(EXAMEN));
    fixture.componentInstance['onRetry']();

    expect(examenService.detail).toHaveBeenCalledTimes(2);
  });

  it("traite un id non numérique comme 'not-found' sans appel réseau", async () => {
    await configure('abc', examenService, apercuStore, detectionService, detectionMasqueStore, authService);
    const fixture = TestBed.createComponent(ExamenDetail);
    fixture.detectChanges();

    expect(examenService.detail).not.toHaveBeenCalled();
    expect(fixture.componentInstance['error']()).toBe('not-found');
  });

  it('le bouton Retour à la liste navigue vers /', async () => {
    await configure('1', examenService, apercuStore, detectionService, detectionMasqueStore, authService);
    const fixture = TestBed.createComponent(ExamenDetail);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture.detectChanges();

    fixture.componentInstance['onRetourListe']();

    expect(navigateSpy).toHaveBeenCalledWith('/');
  });

  it('le clic sur une tuile ouvre le viewer plein écran', async () => {
    await configure('1', examenService, apercuStore, detectionService, detectionMasqueStore, authService);
    const fixture = TestBed.createComponent(ExamenDetail);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-examen-image-viewer')).toBeNull();

    fixture.nativeElement.querySelector('app-examen-image-apercu button').click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-examen-image-viewer')).not.toBeNull();
  });

  it('(fermer) émis par le viewer masque le viewer', async () => {
    await configure('1', examenService, apercuStore, detectionService, detectionMasqueStore, authService);
    const fixture = TestBed.createComponent(ExamenDetail);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('app-examen-image-apercu button').click();
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[aria-label="Fermer"]').click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-examen-image-viewer')).toBeNull();
  });

  it('la destruction du composant appelle ExamenImageApercuStore.clearAll()', async () => {
    await configure('1', examenService, apercuStore, detectionService, detectionMasqueStore, authService);
    const fixture = TestBed.createComponent(ExamenDetail);
    fixture.detectChanges();

    fixture.destroy();

    expect(apercuStore.clearAll).toHaveBeenCalled();
  });

  it('charge les détections de l\'examen à l\'ouverture de la page', async () => {
    const fixture = await createFixture();
    fixture.detectChanges();

    expect(detectionService.lister).toHaveBeenCalledWith(1);
  });

  it('propage les détections chargées aux vignettes et au viewer', async () => {
    (detectionService.lister as ReturnType<typeof vi.fn>).mockReturnValue(of([DETECTION]));
    const fixture = await createFixture();
    fixture.detectChanges();

    expect(fixture.componentInstance['detections']()).toEqual([DETECTION]);

    fixture.nativeElement.querySelector('app-examen-image-apercu button').click();
    fixture.detectChanges();

    const viewer = fixture.nativeElement.querySelector('app-examen-image-viewer');
    expect(viewer).not.toBeNull();
  });

  it('un échec de chargement des détections ne bloque pas l\'affichage de l\'examen', async () => {
    (detectionService.lister as ReturnType<typeof vi.fn>).mockReturnValue(throwError(() => new Error('boom')));
    const fixture = await createFixture();
    fixture.detectChanges();

    expect(fixture.componentInstance['detections']()).toEqual([]);
    expect(fixture.componentInstance['error']()).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('MRN-001');
  });

  it('la destruction du composant appelle DetectionMasqueStore.clearAll()', async () => {
    const fixture = await createFixture();
    fixture.detectChanges();

    fixture.destroy();

    expect(detectionMasqueStore.clearAll).toHaveBeenCalled();
  });

  it("affiche le bouton Lancer l'analyse pour un RADIOLOGUE", async () => {
    const fixture = await createFixture();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain("Lancer l'analyse IA");
  });

  it("affiche le bouton Lancer l'analyse pour un TECHNICIEN", async () => {
    authService = { currentUser: signal({ email: 'tech@test.com', role: 'TECHNICIEN', exp: 9999999999 }) };
    const fixture = await createFixture();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain("Lancer l'analyse IA");
  });

  it("n'affiche pas le bouton Lancer l'analyse pour un ADMIN", async () => {
    authService = { currentUser: signal({ email: 'admin@test.com', role: 'ADMIN', exp: 9999999999 }) };
    const fixture = await createFixture();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain("Lancer l'analyse IA");
  });

  it("cliquer sur Lancer l'analyse appelle le service et passe le statut à EN_COURS", async () => {
    const fixture = await createFixture();
    fixture.detectChanges();

    fixture.componentInstance['onLancerAnalyse']();

    expect(detectionService.lancerAnalyse).toHaveBeenCalledWith(1);
    expect(fixture.componentInstance['statutAnalyse']()).toBe('EN_COURS');
    expect(fixture.componentInstance['analyseEnCours']()).toBe(true);
  });

  it('désactive le bouton quand une analyse est déjà TERMINEE pour cet examen', async () => {
    (detectionService.statutAnalyse as ReturnType<typeof vi.fn>).mockReturnValue(
      of({ examenId: 1, statut: 'TERMINEE', message: null, finieLe: '2026-01-01T00:00:00' })
    );
    const fixture = await createFixture();
    fixture.detectChanges();

    const bouton = fixture.nativeElement.querySelector('button[title="Analyse déjà effectuée pour cet examen"]');
    expect(bouton).not.toBeNull();
    expect((bouton as HTMLButtonElement).disabled).toBe(true);
  });

  it('laisse le bouton actif pour relancer une analyse ECHOUEE', async () => {
    (detectionService.statutAnalyse as ReturnType<typeof vi.fn>).mockReturnValue(
      of({ examenId: 1, statut: 'ECHOUEE', message: 'panne', finieLe: '2026-01-01T00:00:00' })
    );
    const fixture = await createFixture();
    fixture.detectChanges();

    expect(fixture.componentInstance['analyseDejaTerminee']()).toBe(false);

    const boutons = fixture.nativeElement.querySelectorAll('button');
    const bouton = Array.from(boutons).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === "Lancer l'analyse IA"
    ) as HTMLButtonElement;
    expect(bouton.disabled).toBe(false);
  });

  it('peutValider est vrai pour un RADIOLOGUE', async () => {
    const fixture = await createFixture();
    fixture.detectChanges();

    expect(fixture.componentInstance['peutValider']()).toBe(true);
  });

  it('peutValider est faux pour un TECHNICIEN', async () => {
    authService = { currentUser: signal({ email: 'tech@test.com', role: 'TECHNICIEN', exp: 9999999999 }) };
    const fixture = await createFixture();
    fixture.detectChanges();

    expect(fixture.componentInstance['peutValider']()).toBe(false);
  });

  it('onValiderDetection met à jour la détection correspondante avec la réponse du service', async () => {
    (detectionService.lister as ReturnType<typeof vi.fn>).mockReturnValue(of([DETECTION]));
    const fixture = await createFixture();
    fixture.detectChanges();

    fixture.componentInstance['onValiderDetection']({ detectionId: 1, statut: 'ACCEPTEE' });

    expect(detectionService.validerStatut).toHaveBeenCalledWith(1, 1, 'ACCEPTEE');
    expect(fixture.componentInstance['detections']()[0].statut).toBe('ACCEPTEE');
    expect(fixture.componentInstance['detections']()[0].validateurEmail).toBe('radio@test.com');
  });

  it('un échec 409 affiche qu\'une analyse est déjà en cours', async () => {
    (detectionService.lancerAnalyse as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409 }))
    );
    const fixture = await createFixture();
    fixture.detectChanges();

    fixture.componentInstance['onLancerAnalyse']();

    expect(fixture.componentInstance['erreurAnalyse']()).toContain('déjà en cours');
    expect(fixture.componentInstance['analyseEnCours']()).toBe(false);
  });

  it('un échec 422 affiche le message renvoyé par le backend', async () => {
    (detectionService.lancerAnalyse as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 422, error: 'Aucune image dans cet examen' }))
    );
    const fixture = await createFixture();
    fixture.detectChanges();

    fixture.componentInstance['onLancerAnalyse']();

    expect(fixture.componentInstance['erreurAnalyse']()).toBe('Aucune image dans cet examen');
  });

  it('un échec générique affiche un message par défaut', async () => {
    (detectionService.lancerAnalyse as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );
    const fixture = await createFixture();
    fixture.detectChanges();

    fixture.componentInstance['onLancerAnalyse']();

    expect(fixture.componentInstance['erreurAnalyse']()).toContain('Échec du lancement');
  });

  it('reprend le polling automatiquement si une analyse est déjà EN_COURS au chargement', async () => {
    (detectionService.statutAnalyse as ReturnType<typeof vi.fn>).mockReturnValue(
      of({ examenId: 1, statut: 'EN_COURS', message: null, finieLe: null })
    );
    const fixture = await createFixture();
    fixture.detectChanges();

    expect(fixture.componentInstance['analyseEnCours']()).toBe(true);
  });

  it("le polling s'arrête et recharge les détections quand le statut passe à TERMINEE", async () => {
    vi.useFakeTimers();
    try {
      let appelStatut = 0;
      (detectionService.statutAnalyse as ReturnType<typeof vi.fn>).mockImplementation(() => {
        appelStatut++;
        if (appelStatut === 1) {
          return of(STATUT_EN_ATTENTE);
        }
        if (appelStatut < 3) {
          return of({ examenId: 1, statut: 'EN_COURS', message: null, finieLe: null });
        }
        return of({ examenId: 1, statut: 'TERMINEE', message: null, finieLe: '2026-01-01T00:00:00' });
      });

      const fixture = await createFixture();
      fixture.detectChanges();

      fixture.componentInstance['onLancerAnalyse']();
      expect(detectionService.lister).toHaveBeenCalledTimes(1);

      await vi.advanceTimersByTimeAsync(3000);
      expect(fixture.componentInstance['statutAnalyse']()).toBe('EN_COURS');

      await vi.advanceTimersByTimeAsync(3000);
      expect(fixture.componentInstance['statutAnalyse']()).toBe('TERMINEE');
      expect(detectionService.lister).toHaveBeenCalledTimes(2);
    } finally {
      vi.useRealTimers();
    }
  });

  it('la destruction du composant arrête le polling en cours', async () => {
    vi.useFakeTimers();
    try {
      (detectionService.statutAnalyse as ReturnType<typeof vi.fn>).mockReturnValue(
        of({ examenId: 1, statut: 'EN_COURS', message: null, finieLe: null })
      );
      const fixture = await createFixture();
      fixture.detectChanges();

      fixture.componentInstance['onLancerAnalyse']();
      const appelsAvantDestroy = (detectionService.statutAnalyse as ReturnType<typeof vi.fn>).mock.calls.length;

      fixture.destroy();
      await vi.advanceTimersByTimeAsync(10000);

      expect((detectionService.statutAnalyse as ReturnType<typeof vi.fn>).mock.calls.length).toBe(
        appelsAvantDestroy
      );
    } finally {
      vi.useRealTimers();
    }
  });
});
