import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Detection } from '../../models/detection.model';
import { ExamenImage } from '../../models/examen.model';
import { ApercuEntree, ExamenImageApercuStore } from '../../services/examen-image-apercu-store.service';
import { ExamenImageApercu } from './examen-image-apercu';

const IMAGE_DISPONIBLE: ExamenImage = { imageId: 1, format: 'PNG', apercuDisponible: true, ordre: 0 };
const IMAGE_INDISPONIBLE: ExamenImage = { imageId: 2, format: 'DICOM', apercuDisponible: false, ordre: 1 };

const DETECTION_SUR_IMAGE_1: Detection = {
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

describe('ExamenImageApercu', () => {
  let store: Partial<ExamenImageApercuStore>;

  function mockStore(entree: ApercuEntree | undefined): void {
    store = {
      charger: vi.fn(),
      entree: vi.fn().mockReturnValue(signal(entree)),
    };
  }

  async function createComponent(image: ExamenImage, detections: Detection[] = []) {
    await TestBed.configureTestingModule({
      imports: [ExamenImageApercu],
      providers: [{ provide: ExamenImageApercuStore, useValue: store }],
    }).compileComponents();

    const fixture = TestBed.createComponent(ExamenImageApercu);
    fixture.componentRef.setInput('examenId', 10);
    fixture.componentRef.setInput('image', image);
    fixture.componentRef.setInput('detections', detections);
    fixture.detectChanges();
    return fixture;
  }

  it('délègue le chargement au store lors de ngOnInit', async () => {
    mockStore({ objectUrl: 'blob:fake-url', loading: false, error: false });

    await createComponent(IMAGE_DISPONIBLE);

    expect(store.charger).toHaveBeenCalledWith(10, IMAGE_DISPONIBLE);
  });

  it("affiche l'image quand le store expose un aperçu chargé avec succès", async () => {
    mockStore({ objectUrl: 'blob:fake-url', loading: false, error: false });

    const fixture = await createComponent(IMAGE_DISPONIBLE);

    const img = fixture.nativeElement.querySelector('img');
    expect(img?.getAttribute('src')).toBe('blob:fake-url');
  });

  it("affiche 'Chargement…' pendant que le store charge l'aperçu", async () => {
    mockStore({ objectUrl: null, loading: true, error: false });

    const fixture = await createComponent(IMAGE_DISPONIBLE);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Chargement…');
  });

  it("affiche 'Aperçu indisponible' sans appeler le store quand apercuDisponible est false", async () => {
    mockStore(undefined);

    const fixture = await createComponent(IMAGE_INDISPONIBLE);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aperçu indisponible');
  });

  it("affiche une erreur quand le store expose error: true", async () => {
    mockStore({ objectUrl: null, loading: false, error: true });

    const fixture = await createComponent(IMAGE_DISPONIBLE);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Erreur de chargement');
  });

  it("le wrapper est un <button> uniquement quand l'image est ouvrable", async () => {
    mockStore({ objectUrl: 'blob:fake-url', loading: false, error: false });

    const fixture = await createComponent(IMAGE_DISPONIBLE);

    expect(fixture.nativeElement.querySelector('button')).not.toBeNull();
  });

  it("le wrapper reste un <div> quand l'image n'est pas ouvrable", async () => {
    mockStore(undefined);

    const fixture = await createComponent(IMAGE_INDISPONIBLE);

    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });

  it("émet ouvrir au clic quand l'image est ouvrable", async () => {
    mockStore({ objectUrl: 'blob:fake-url', loading: false, error: false });

    const fixture = await createComponent(IMAGE_DISPONIBLE);
    const emitSpy = vi.fn();
    fixture.componentInstance.ouvrir.subscribe(emitSpy);

    fixture.nativeElement.querySelector('button')!.click();

    expect(emitSpy).toHaveBeenCalledTimes(1);
  });

  it("n'émet pas ouvrir quand apercuDisponible est false", async () => {
    mockStore(undefined);

    const fixture = await createComponent(IMAGE_INDISPONIBLE);
    const emitSpy = vi.fn();
    fixture.componentInstance.ouvrir.subscribe(emitSpy);

    fixture.componentInstance['onClick']();

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it("n'émet pas ouvrir tant que le chargement est en cours", async () => {
    mockStore({ objectUrl: null, loading: true, error: false });

    const fixture = await createComponent(IMAGE_DISPONIBLE);
    const emitSpy = vi.fn();
    fixture.componentInstance.ouvrir.subscribe(emitSpy);

    fixture.componentInstance['onClick']();

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it("n'émet pas ouvrir quand le store est en erreur", async () => {
    mockStore({ objectUrl: null, loading: false, error: true });

    const fixture = await createComponent(IMAGE_DISPONIBLE);
    const emitSpy = vi.fn();
    fixture.componentInstance.ouvrir.subscribe(emitSpy);

    fixture.componentInstance['onClick']();

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('affiche le badge de détection quand une détection référence cette image', async () => {
    mockStore({ objectUrl: 'blob:fake-url', loading: false, error: false });

    const fixture = await createComponent(IMAGE_DISPONIBLE, [DETECTION_SUR_IMAGE_1]);

    expect(fixture.nativeElement.querySelector('[aria-label="Détection IA présente"]')).not.toBeNull();
  });

  it("n'affiche pas le badge quand aucune détection ne référence cette image", async () => {
    mockStore({ objectUrl: 'blob:fake-url', loading: false, error: false });

    const fixture = await createComponent(IMAGE_DISPONIBLE, []);

    expect(fixture.nativeElement.querySelector('[aria-label="Détection IA présente"]')).toBeNull();
  });
});
