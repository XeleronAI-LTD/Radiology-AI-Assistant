import { Component, OnInit, Signal, computed, inject, input, output } from '@angular/core';
import { Detection } from '../../models/detection.model';
import { ExamenImage } from '../../models/examen.model';
import { ApercuEntree, ExamenImageApercuStore } from '../../services/examen-image-apercu-store.service';

@Component({
  selector: 'app-examen-image-apercu',
  imports: [],
  templateUrl: './examen-image-apercu.html',
})
export class ExamenImageApercu implements OnInit {
  private readonly store = inject(ExamenImageApercuStore);

  readonly examenId = input.required<number>();
  readonly image = input.required<ExamenImage>();
  readonly detections = input<Detection[]>([]);
  readonly ouvrir = output<void>();

  protected entree!: Signal<ApercuEntree | undefined>;

  protected readonly aUneDetection = computed(() =>
    this.detections().some((d) => d.imageId === this.image().imageId)
  );

  ngOnInit(): void {
    this.store.charger(this.examenId(), this.image());
    this.entree = this.store.entree(this.image().imageId);
  }

  protected onClick(): void {
    const e = this.entree();
    if (this.image().apercuDisponible && e && !e.loading && !e.error) {
      this.ouvrir.emit();
    }
  }
}
