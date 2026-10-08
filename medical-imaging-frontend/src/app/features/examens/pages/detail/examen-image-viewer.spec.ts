import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Detection } from '../../models/detection.model';
import { ExamenImage } from '../../models/examen.model';
import { ApercuEntree, ExamenImageApercuStore } from '../../services/examen-image-apercu-store.service';
import { DetectionMasqueStore, MasqueEntree } from '../../services/detection-masque-store.service';
import { ExamenImageViewer } from './examen-image-viewer';

const IMAGES: ExamenImage[] = [
  { imageId: 1, format: 'PNG', apercuDisponible: true, ordre: 0 },
  { imageId: 2, format: 'PNG', apercuDisponible: false, ordre: 1 },
  { imageId: 3, format: 'PNG', apercuDisponible: true, ordre: 2 },
  { imageId: 4, format: 'PNG', apercuDisponible: true, ordre: 3 },
];

const ENTREES: Record<number, ApercuEntree | undefined> = {
  1: { objectUrl: 'blob:1', loading: false, error: false },
  3: { objectUrl: 'blob:3', loading: false, error: false },
  4: { objectUrl: null, loading: false, error: true },
};

const DETECTION_BOX: Detection = {
  id: 10,
  imageId: 1,
  type: 'BOX',
  anomalie: 'nodule',
  confiance: 0.87,
  statut: 'EN_ATTENTE',
  coupe: 0,
  bbox: { x: 5, y: 6, largeur: 20, hauteur: 30 },
  apercuMasqueDisponible: false,
  validateurEmail: null,
  valideLe: null,
};

const DETECTION_MASQUE: Detection = {
  id: 20,
  imageId: 1,
  type: 'MASQUE',
  anomalie: 'rate',
  confiance: 0.93,
  statut: 'EN_ATTENTE',
  coupe: 0,
  bbox: null,
  apercuMasqueDisponible: true,
  validateurEmail: null,
  valideLe: null,
};

describe('ExamenImageViewer', () => {
  let store: Partial<ExamenImageApercuStore>;
  let masqueStore: Partial<DetectionMasqueStore>;
  let masqueEntrees: Record<number, MasqueEntree | undefined>;

  beforeEach(() => {
    store = {
      charger: vi.fn(),
      entree: vi.fn((imageId: number) => signal(ENTREES[imageId])),
    };

    masqueEntrees = {};
    masqueStore = {
      charger: vi.fn(),
      clearAll: vi.fn(),
      entree: vi.fn((detectionId: number) => signal(masqueEntrees[detectionId])),
    };
  });

  async function createComponent(
    indexInitial: number,
    detections: Detection[] = [],
    peutValider = false
  ) {
    await TestBed.configureTestingModule({
      imports: [ExamenImageViewer],
      providers: [
        { provide: ExamenImageApercuStore, useValue: store },
        { provide: DetectionMasqueStore, useValue: masqueStore },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ExamenImageViewer);
    fixture.componentRef.setInput('examenId', 10);
    fixture.componentRef.setInput('images', IMAGES);
    fixture.componentRef.setInput('indexInitial', indexInitial);
    fixture.componentRef.setInput('detections', detections);
    fixture.componentRef.setInput('peutValider', peutValider);
    fixture.detectChanges();
    return fixture;
  }

  it("s'ouvre sur l'image d'index initial", async () => {
    const fixture = await createComponent(0);

    expect(fixture.componentInstance['imageCourante']().imageId).toBe(1);
  });

  it('le zoom (boutons) reste borné entre 1 et 5', async () => {
    const fixture = await createComponent(0);
    const instance = fixture.componentInstance;

    for (let i = 0; i < 30; i++) {
      instance['onZoomIn']();
    }
    expect(instance['niveauZoom']()).toBe(5);

    for (let i = 0; i < 30; i++) {
      instance['onZoomOut']();
    }
    expect(instance['niveauZoom']()).toBe(1);
  });

  it('le zoom (molette) reste borné entre 1 et 5', async () => {
    const fixture = await createComponent(0);
    const instance = fixture.componentInstance;

    for (let i = 0; i < 60; i++) {
      instance['onWheel']({ deltaY: -1, preventDefault: () => {} } as WheelEvent);
    }
    expect(instance['niveauZoom']()).toBe(5);
  });

  it('le pan reste inactif tant que le zoom est à 1', async () => {
    const fixture = await createComponent(0);
    const instance = fixture.componentInstance;

    instance['onPointerDown'](new MouseEvent('mousedown', { clientX: 0, clientY: 0 }));
    window.dispatchEvent(new MouseEvent('mousemove', { clientX: 50, clientY: 50 }));

    expect(instance['pan']()).toEqual({ x: 0, y: 0 });
  });

  it('réinitialise le zoom et le pan via onReset', async () => {
    const fixture = await createComponent(0);
    const instance = fixture.componentInstance;

    instance['onZoomIn']();
    instance['pan'].set({ x: 10, y: 10 });

    instance['onReset']();

    expect(instance['niveauZoom']()).toBe(1);
    expect(instance['pan']()).toEqual({ x: 0, y: 0 });
  });

  it('onSuivant saute automatiquement les images non ouvrables', async () => {
    const fixture = await createComponent(0);
    const instance = fixture.componentInstance;

    instance['onSuivant'](); // image 2 non ouvrable (apercuDisponible: false) -> saute vers image 3

    expect(instance['imageCourante']().imageId).toBe(3);
  });

  it('onSuivant ne change rien quand aucune image ouvrable ne suit', async () => {
    const fixture = await createComponent(2); // image 3 (index 2)
    const instance = fixture.componentInstance;

    instance['onSuivant'](); // image 4 est en erreur -> aucune image ouvrable après

    expect(instance['imageCourante']().imageId).toBe(3);
  });

  it("onPrecedent ne change rien en butée sur la première image", async () => {
    const fixture = await createComponent(0);
    const instance = fixture.componentInstance;

    instance['onPrecedent']();

    expect(instance['imageCourante']().imageId).toBe(1);
  });

  it("les boutons précédent/suivant sont désactivés en butée", async () => {
    const fixture = await createComponent(2); // image 3 : rien avant sauf via saut, rien d'ouvrable après

    const boutons = fixture.nativeElement.querySelectorAll('button[aria-label="Image précédente"], button[aria-label="Image suivante"]');
    const [precedent, suivant] = Array.from(boutons) as HTMLButtonElement[];

    expect(precedent.disabled).toBe(false); // l'image 1 est ouvrable en remontant
    expect(suivant.disabled).toBe(true); // image 4 en erreur, rien après
  });

  it("changer d'image réinitialise systématiquement le zoom et le pan", async () => {
    const fixture = await createComponent(0);
    const instance = fixture.componentInstance;

    instance['onZoomIn']();
    instance['pan'].set({ x: 20, y: 20 });

    instance['onSuivant']();

    expect(instance['niveauZoom']()).toBe(1);
    expect(instance['pan']()).toEqual({ x: 0, y: 0 });
  });

  it('émet fermer au clic sur le bouton ✕', async () => {
    const fixture = await createComponent(0);
    const emitSpy = vi.fn();
    fixture.componentInstance.fermer.subscribe(emitSpy);

    fixture.nativeElement.querySelector('[aria-label="Fermer"]').click();

    expect(emitSpy).toHaveBeenCalled();
  });

  it('émet fermer au clic sur le fond', async () => {
    const fixture = await createComponent(0);
    const emitSpy = vi.fn();
    fixture.componentInstance.fermer.subscribe(emitSpy);

    fixture.nativeElement.querySelector('[role="dialog"]').click();

    expect(emitSpy).toHaveBeenCalled();
  });

  it('émet fermer sur la touche Échap', async () => {
    const fixture = await createComponent(0);
    const emitSpy = vi.fn();
    fixture.componentInstance.fermer.subscribe(emitSpy);

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(emitSpy).toHaveBeenCalled();
  });

  it("n'émet pas fermer au clic sur l'image", async () => {
    const fixture = await createComponent(0);
    const emitSpy = vi.fn();
    fixture.componentInstance.fermer.subscribe(emitSpy);

    fixture.nativeElement.querySelector('img').click();

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it("n'émet plus fermer sur Échap après la destruction du composant", async () => {
    const fixture = await createComponent(0);
    const emitSpy = vi.fn();
    fixture.componentInstance.fermer.subscribe(emitSpy);

    fixture.destroy();
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('affiche un rectangle pour une détection BOX de la coupe courante', async () => {
    const fixture = await createComponent(0, [DETECTION_BOX]);
    fixture.componentInstance['onImageLoad']({ naturalWidth: 100, naturalHeight: 100 } as HTMLImageElement);
    fixture.detectChanges();

    const rect = fixture.nativeElement.querySelector('rect');
    expect(rect).not.toBeNull();
    expect(rect.getAttribute('x')).toBe('5');
    expect(rect.getAttribute('y')).toBe('6');
    expect(rect.getAttribute('width')).toBe('20');
    expect(rect.getAttribute('height')).toBe('30');
  });

  it("n'affiche pas de rectangle tant que les dimensions naturelles de l'image ne sont pas connues", async () => {
    const fixture = await createComponent(0, [DETECTION_BOX]);

    expect(fixture.nativeElement.querySelector('rect')).toBeNull();
  });

  it('affiche un calque coloré pour une détection MASQUE avec une URL de masque résolue', async () => {
    masqueEntrees[20] = { objectUrl: 'blob:masque-20', loading: false, error: false };

    const fixture = await createComponent(0, [DETECTION_MASQUE]);

    const calque = fixture.nativeElement.querySelector('.bg-red-600');
    expect(calque).not.toBeNull();
    expect(calque.getAttribute('style')).toContain('blob:masque-20');
  });

  it("n'affiche pas de calque MASQUE tant que son URL n'est pas résolue", async () => {
    const fixture = await createComponent(0, [DETECTION_MASQUE]);

    expect(fixture.nativeElement.querySelector('.bg-red-600')).toBeNull();
  });

  it("n'affiche aucun overlay quand la coupe courante n'a aucune détection", async () => {
    const fixture = await createComponent(0, []);

    expect(fixture.nativeElement.querySelector('rect')).toBeNull();
    expect(fixture.nativeElement.querySelector('.bg-red-600')).toBeNull();
  });

  it("charge les masques de la coupe initiale dès l'ouverture", async () => {
    await createComponent(0, [DETECTION_MASQUE]);

    expect(masqueStore.charger).toHaveBeenCalledWith(10, DETECTION_MASQUE);
  });

  it('change de coupe : vide le store des masques puis recharge ceux de la nouvelle coupe', async () => {
    const detectionCoupe3: Detection = { ...DETECTION_MASQUE, id: 21, imageId: 3 };
    const fixture = await createComponent(0, [DETECTION_MASQUE, detectionCoupe3]);

    (masqueStore.charger as ReturnType<typeof vi.fn>).mockClear();
    (masqueStore.clearAll as ReturnType<typeof vi.fn>).mockClear();

    fixture.componentInstance['onSuivant'](); // image 2 non ouvrable -> saute vers image 3

    expect(masqueStore.clearAll).toHaveBeenCalledTimes(1);
    expect(masqueStore.charger).toHaveBeenCalledWith(10, detectionCoupe3);
  });

  it("n'affiche pas les boutons Accepter/Rejeter quand peutValider est faux", async () => {
    masqueEntrees[20] = { objectUrl: 'blob:masque-20', loading: false, error: false };
    const fixture = await createComponent(0, [DETECTION_BOX, DETECTION_MASQUE], false);

    const texte = (fixture.nativeElement as HTMLElement).textContent;
    expect(texte).not.toContain('Accepter');
    expect(texte).not.toContain('Rejeter');
  });

  it('affiche les boutons Accepter/Rejeter pour une box et un masque quand peutValider est vrai', async () => {
    masqueEntrees[20] = { objectUrl: 'blob:masque-20', loading: false, error: false };
    const fixture = await createComponent(0, [DETECTION_BOX, DETECTION_MASQUE], true);

    const boutons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const libelles = boutons.map((b) => b.textContent?.trim());
    expect(libelles.filter((l) => l === 'Accepter')).toHaveLength(2);
    expect(libelles.filter((l) => l === 'Rejeter')).toHaveLength(2);
  });

  it('le clic sur Accepter émet validerDetection avec le statut ACCEPTEE', async () => {
    const fixture = await createComponent(0, [DETECTION_BOX], true);
    const emitSpy = vi.fn();
    fixture.componentInstance.validerDetection.subscribe(emitSpy);

    const bouton = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>).find(
      (b) => b.textContent?.trim() === 'Accepter'
    )!;
    bouton.click();

    expect(emitSpy).toHaveBeenCalledWith({ detectionId: DETECTION_BOX.id, statut: 'ACCEPTEE' });
  });

  it('le clic sur Rejeter émet validerDetection avec le statut REJETEE', async () => {
    const fixture = await createComponent(0, [DETECTION_BOX], true);
    const emitSpy = vi.fn();
    fixture.componentInstance.validerDetection.subscribe(emitSpy);

    const bouton = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>).find(
      (b) => b.textContent?.trim() === 'Rejeter'
    )!;
    bouton.click();

    expect(emitSpy).toHaveBeenCalledWith({ detectionId: DETECTION_BOX.id, statut: 'REJETEE' });
  });

  it('une box ACCEPTEE est tracée en vert', async () => {
    const fixture = await createComponent(0, [{ ...DETECTION_BOX, statut: 'ACCEPTEE' }]);
    fixture.componentInstance['onImageLoad']({ naturalWidth: 100, naturalHeight: 100 } as HTMLImageElement);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('rect').getAttribute('stroke')).toBe('#16a34a');
  });

  it('une box REJETEE est tracée en rouge et son libellé est barré', async () => {
    const fixture = await createComponent(0, [{ ...DETECTION_BOX, statut: 'REJETEE' }]);
    fixture.componentInstance['onImageLoad']({ naturalWidth: 100, naturalHeight: 100 } as HTMLImageElement);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('rect').getAttribute('stroke')).toBe('#dc2626');
    expect(fixture.nativeElement.querySelector('text').classList.contains('line-through')).toBe(true);
  });

  it('affiche "validé par" quand validateurEmail est renseigné', async () => {
    const fixture = await createComponent(0, [
      { ...DETECTION_BOX, statut: 'ACCEPTEE', validateurEmail: 'radio@test.com' },
    ]);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('validé par radio@test.com');
  });
});
