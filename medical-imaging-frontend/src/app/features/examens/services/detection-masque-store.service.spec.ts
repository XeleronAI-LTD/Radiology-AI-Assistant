import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Detection } from '../models/detection.model';
import { DetectionService } from './detection.service';
import { DetectionMasqueStore } from './detection-masque-store.service';

const DETECTION_MASQUE: Detection = {
  id: 1,
  imageId: 101,
  type: 'MASQUE',
  anomalie: 'rate',
  confiance: 0.9,
  statut: 'EN_ATTENTE',
  coupe: 5,
  bbox: null,
  apercuMasqueDisponible: true,
  validateurEmail: null,
  valideLe: null,
};

const DETECTION_MASQUE_SANS_APERCU: Detection = {
  ...DETECTION_MASQUE,
  id: 2,
  apercuMasqueDisponible: false,
};

const DETECTION_BOX: Detection = {
  id: 3,
  imageId: 102,
  type: 'BOX',
  anomalie: 'nodule',
  confiance: 0.8,
  statut: 'EN_ATTENTE',
  coupe: 2,
  bbox: { x: 1, y: 2, largeur: 3, hauteur: 4 },
  apercuMasqueDisponible: false,
  validateurEmail: null,
  valideLe: null,
};

describe('DetectionMasqueStore', () => {
  let detectionService: Partial<DetectionService>;
  let store: DetectionMasqueStore;
  let createObjectURLSpy: ReturnType<typeof vi.fn>;
  let revokeObjectURLSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    detectionService = { masque: vi.fn() };

    createObjectURLSpy = vi.fn().mockReturnValue('blob:fake-url');
    revokeObjectURLSpy = vi.fn();
    vi.stubGlobal('URL', { ...URL, createObjectURL: createObjectURLSpy, revokeObjectURL: revokeObjectURLSpy });

    TestBed.configureTestingModule({
      providers: [{ provide: DetectionService, useValue: detectionService }],
    });
    store = TestBed.inject(DetectionMasqueStore);
  });

  afterEach(() => vi.unstubAllGlobals());

  it('charge un masque disponible et remplit l\'entrée', () => {
    (detectionService.masque as ReturnType<typeof vi.fn>).mockReturnValue(of(new Blob(['x'])));

    store.charger(42, DETECTION_MASQUE);

    expect(detectionService.masque).toHaveBeenCalledWith(42, 1);
    expect(store.entree(1)()).toEqual({ objectUrl: 'blob:fake-url', loading: false, error: false });
  });

  it("n'appelle pas le service pour une détection de type BOX", () => {
    store.charger(42, DETECTION_BOX);

    expect(detectionService.masque).not.toHaveBeenCalled();
    expect(store.entree(3)()).toBeUndefined();
  });

  it("n'appelle pas le service quand apercuMasqueDisponible est false", () => {
    store.charger(42, DETECTION_MASQUE_SANS_APERCU);

    expect(detectionService.masque).not.toHaveBeenCalled();
    expect(store.entree(2)()).toBeUndefined();
  });

  it("ne relance pas d'appel si une entrée existe déjà pour cette détection", () => {
    (detectionService.masque as ReturnType<typeof vi.fn>).mockReturnValue(of(new Blob(['x'])));

    store.charger(42, DETECTION_MASQUE);
    store.charger(42, DETECTION_MASQUE);

    expect(detectionService.masque).toHaveBeenCalledTimes(1);
  });

  it('passe l\'entrée en erreur en cas d\'échec du service', () => {
    (detectionService.masque as ReturnType<typeof vi.fn>).mockReturnValue(throwError(() => new Error('boom')));

    store.charger(42, DETECTION_MASQUE);

    expect(store.entree(1)()).toEqual({ objectUrl: null, loading: false, error: true });
  });

  it('clearAll révoque les URLs objet et vide le cache', () => {
    (detectionService.masque as ReturnType<typeof vi.fn>).mockReturnValue(of(new Blob(['x'])));
    store.charger(42, DETECTION_MASQUE);

    store.clearAll();

    expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:fake-url');
    expect(store.entree(1)()).toBeUndefined();
  });
});
