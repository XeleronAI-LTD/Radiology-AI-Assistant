import { NgStyle } from '@angular/common';
import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { Detection, DetectionStatut } from '../../models/detection.model';
import { ExamenImage } from '../../models/examen.model';
import { ExamenImageApercuStore } from '../../services/examen-image-apercu-store.service';
import { DetectionMasqueStore } from '../../services/detection-masque-store.service';

const ZOOM_MIN = 1;
const ZOOM_MAX = 5;
const ZOOM_STEP_BOUTON = 0.25;
const ZOOM_STEP_MOLETTE = 0.1;

function clamp(valeur: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, valeur));
}

@Component({
  selector: 'app-examen-image-viewer',
  imports: [NgStyle],
  templateUrl: './examen-image-viewer.html',
})
export class ExamenImageViewer implements OnInit, OnDestroy {
  private readonly store = inject(ExamenImageApercuStore);
  private readonly masqueStore = inject(DetectionMasqueStore);

  readonly examenId = input.required<number>();
  readonly images = input.required<ExamenImage[]>();
  readonly indexInitial = input.required<number>();
  readonly detections = input<Detection[]>([]);
  readonly peutValider = input<boolean>(false);
  readonly fermer = output<void>();
  readonly validerDetection = output<{ detectionId: number; statut: 'ACCEPTEE' | 'REJETEE' }>();

  private readonly conteneurImage = viewChild<ElementRef<HTMLElement>>('conteneurImage');

  protected readonly indexCourant = signal(0);
  protected readonly niveauZoom = signal(ZOOM_MIN);
  protected readonly pan = signal({ x: 0, y: 0 });
  protected readonly naturalSize = signal<{ width: number; height: number } | null>(null);

  private enTrainDeDeplacer = false;
  private dernierPointeur = { x: 0, y: 0 };

  private readonly onKeydown = (event: KeyboardEvent): void => this.gererClavier(event);
  private readonly onMouseMove = (event: MouseEvent): void => this.gererDeplacement(event);
  private readonly onMouseUp = (): void => this.arreterDeplacement();

  protected readonly imageCourante = computed<ExamenImage>(() => this.images()[this.indexCourant()]);

  protected readonly entreeCourante = computed(() => this.store.entree(this.imageCourante().imageId)());

  protected readonly transformStyle = computed(
    () => `scale(${this.niveauZoom()}) translate(${this.pan().x}px, ${this.pan().y}px)`
  );

  protected readonly zoomPourcentage = computed(() => Math.round(this.niveauZoom() * 100));

  protected readonly indexPrecedentOuvrable = computed(() => this.chercherIndexOuvrable(-1));
  protected readonly indexSuivantOuvrable = computed(() => this.chercherIndexOuvrable(1));

  protected readonly position = computed(
    () => `Image ${this.imageCourante().ordre} — ${this.indexCourant() + 1} / ${this.images().length}`
  );

  protected readonly detectionsCoupeCourante = computed(() =>
    this.detections().filter((d) => d.imageId === this.imageCourante().imageId)
  );

  protected readonly boxes = computed(() => this.detectionsCoupeCourante().filter((d) => d.type === 'BOX'));

  protected readonly masques = computed(() =>
    this.detectionsCoupeCourante().filter((d) => d.type === 'MASQUE')
  );

  ngOnInit(): void {
    this.indexCourant.set(this.indexInitial());
    this.store.charger(this.examenId(), this.imageCourante());
    this.chargerMasquesCoupeCourante();

    window.addEventListener('keydown', this.onKeydown);
    window.addEventListener('mousemove', this.onMouseMove);
    window.addEventListener('mouseup', this.onMouseUp);
    window.addEventListener('mouseleave', this.onMouseUp);
  }

  ngOnDestroy(): void {
    window.removeEventListener('keydown', this.onKeydown);
    window.removeEventListener('mousemove', this.onMouseMove);
    window.removeEventListener('mouseup', this.onMouseUp);
    window.removeEventListener('mouseleave', this.onMouseUp);
  }

  protected onFermer(): void {
    this.fermer.emit();
  }

  protected onArretPropagation(event: MouseEvent): void {
    event.stopPropagation();
  }

  protected onZoomIn(): void {
    this.niveauZoom.update((z) => clamp(z + ZOOM_STEP_BOUTON, ZOOM_MIN, ZOOM_MAX));
  }

  protected onZoomOut(): void {
    this.niveauZoom.update((z) => clamp(z - ZOOM_STEP_BOUTON, ZOOM_MIN, ZOOM_MAX));
  }

  protected onReset(): void {
    this.niveauZoom.set(ZOOM_MIN);
    this.pan.set({ x: 0, y: 0 });
  }

  protected onWheel(event: WheelEvent): void {
    event.preventDefault();
    const delta = event.deltaY < 0 ? ZOOM_STEP_MOLETTE : -ZOOM_STEP_MOLETTE;
    this.niveauZoom.update((z) => clamp(z + delta, ZOOM_MIN, ZOOM_MAX));
  }

  protected onPointerDown(event: MouseEvent): void {
    if (this.niveauZoom() <= ZOOM_MIN) {
      return;
    }
    this.enTrainDeDeplacer = true;
    this.dernierPointeur = { x: event.clientX, y: event.clientY };
  }

  protected onPrecedent(): void {
    this.allerA(this.indexPrecedentOuvrable());
  }

  protected onSuivant(): void {
    this.allerA(this.indexSuivantOuvrable());
  }

  private allerA(index: number | null): void {
    if (index === null) {
      return;
    }
    this.indexCourant.set(index);
    this.store.charger(this.examenId(), this.imageCourante());
    this.naturalSize.set(null);
    this.masqueStore.clearAll();
    this.chargerMasquesCoupeCourante();
    this.niveauZoom.set(ZOOM_MIN);
    this.pan.set({ x: 0, y: 0 });
  }

  private chargerMasquesCoupeCourante(): void {
    for (const detection of this.masques()) {
      this.masqueStore.charger(this.examenId(), detection);
    }
  }

  protected masqueUrl(detectionId: number): string | null {
    return this.masqueStore.entree(detectionId)()?.objectUrl ?? null;
  }

  protected pourcentage(confiance: number): number {
    return Math.round(confiance * 100);
  }

  protected onAccepter(detectionId: number): void {
    this.validerDetection.emit({ detectionId, statut: 'ACCEPTEE' });
  }

  protected onRejeter(detectionId: number): void {
    this.validerDetection.emit({ detectionId, statut: 'REJETEE' });
  }

  protected couleurBox(statut: DetectionStatut): string {
    if (statut === 'ACCEPTEE') {
      return '#16a34a';
    }
    if (statut === 'REJETEE') {
      return '#dc2626';
    }
    return '#f59e0b';
  }

  protected classeMasque(statut: DetectionStatut): string {
    return statut === 'ACCEPTEE' ? 'bg-green-600' : 'bg-red-600';
  }

  protected onImageLoad(img: HTMLImageElement): void {
    this.naturalSize.set({ width: img.naturalWidth, height: img.naturalHeight });
  }

  private gererDeplacement(event: MouseEvent): void {
    if (!this.enTrainDeDeplacer) {
      return;
    }
    const deltaX = event.clientX - this.dernierPointeur.x;
    const deltaY = event.clientY - this.dernierPointeur.y;
    this.dernierPointeur = { x: event.clientX, y: event.clientY };

    const conteneur = this.conteneurImage()?.nativeElement;
    const maxOffsetX = conteneur ? ((this.niveauZoom() - 1) * conteneur.clientWidth) / 2 : Infinity;
    const maxOffsetY = conteneur ? ((this.niveauZoom() - 1) * conteneur.clientHeight) / 2 : Infinity;

    this.pan.update((p) => ({
      x: clamp(p.x + deltaX, -maxOffsetX, maxOffsetX),
      y: clamp(p.y + deltaY, -maxOffsetY, maxOffsetY),
    }));
  }

  private arreterDeplacement(): void {
    this.enTrainDeDeplacer = false;
  }

  private gererClavier(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.fermer.emit();
    } else if (event.key === 'ArrowLeft') {
      this.onPrecedent();
    } else if (event.key === 'ArrowRight') {
      this.onSuivant();
    }
  }

  private estOuvrable(image: ExamenImage): boolean {
    if (!image.apercuDisponible) {
      return false;
    }
    const entree = this.store.entree(image.imageId)();
    return entree !== undefined && !entree.loading && !entree.error;
  }

  private chercherIndexOuvrable(direction: 1 | -1): number | null {
    const liste = this.images();
    for (let i = this.indexCourant() + direction; i >= 0 && i < liste.length; i += direction) {
      if (this.estOuvrable(liste[i])) {
        return i;
      }
    }
    return null;
  }
}
