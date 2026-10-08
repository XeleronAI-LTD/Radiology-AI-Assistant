import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { ExamenImage } from '../models/examen.model';
import { ExamenService } from './examen.service';
import { ExamenImageApercuStore } from './examen-image-apercu-store.service';

const IMAGE_DISPONIBLE: ExamenImage = { imageId: 1, format: 'PNG', apercuDisponible: true, ordre: 0 };
const IMAGE_INDISPONIBLE: ExamenImage = { imageId: 2, format: 'DICOM', apercuDisponible: false, ordre: 1 };

describe('ExamenImageApercuStore', () => {
  let examenService: Partial<ExamenService>;
  let store: ExamenImageApercuStore;
  let createObjectURLSpy: ReturnType<typeof vi.fn>;
  let revokeObjectURLSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    examenService = { apercu: vi.fn() };

    createObjectURLSpy = vi.fn().mockReturnValue('blob:fake-url');
    revokeObjectURLSpy = vi.fn();
    vi.stubGlobal('URL', { ...URL, createObjectURL: createObjectURLSpy, revokeObjectURL: revokeObjectURLSpy });

    TestBed.configureTestingModule({
      providers: [{ provide: ExamenService, useValue: examenService }],
    });
    store = TestBed.inject(ExamenImageApercuStore);
  });

  afterEach(() => vi.unstubAllGlobals());

  it("charge un aperçu disponible et remplit l'entrée", () => {
    (examenService.apercu as ReturnType<typeof vi.fn>).mockReturnValue(of(new Blob(['x'])));

    store.charger(10, IMAGE_DISPONIBLE);

    expect(examenService.apercu).toHaveBeenCalledWith(10, 1);
    expect(store.entree(1)()).toEqual({ objectUrl: 'blob:fake-url', loading: false, error: false });
  });

  it("ne relance pas d'appel HTTP si un chargement est déjà en cours pour la même image", () => {
    const subject = new Subject<Blob>();
    (examenService.apercu as ReturnType<typeof vi.fn>).mockReturnValue(subject);

    store.charger(10, IMAGE_DISPONIBLE);
    store.charger(10, IMAGE_DISPONIBLE);

    expect(examenService.apercu).toHaveBeenCalledTimes(1);
    expect(store.entree(1)()?.loading).toBe(true);
  });

  it("n'appelle pas l'endpoint gardien quand apercuDisponible est false", () => {
    store.charger(10, IMAGE_INDISPONIBLE);

    expect(examenService.apercu).not.toHaveBeenCalled();
    expect(store.entree(2)()).toBeUndefined();
  });

  it("passe l'entrée en erreur en cas d'échec de l'endpoint gardien", () => {
    (examenService.apercu as ReturnType<typeof vi.fn>).mockReturnValue(throwError(() => new Error('boom')));

    store.charger(10, IMAGE_DISPONIBLE);

    expect(store.entree(1)()).toEqual({ objectUrl: null, loading: false, error: true });
  });

  it('clearAll révoque les URLs objet et vide le cache', () => {
    (examenService.apercu as ReturnType<typeof vi.fn>).mockReturnValue(of(new Blob(['x'])));
    store.charger(10, IMAGE_DISPONIBLE);

    store.clearAll();

    expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:fake-url');
    expect(store.entree(1)()).toBeUndefined();
  });
});
