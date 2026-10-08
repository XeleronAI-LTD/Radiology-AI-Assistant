import { Injectable, Signal, computed, inject, signal } from '@angular/core';
import { ExamenImage } from '../models/examen.model';
import { ExamenService } from './examen.service';

export interface ApercuEntree {
  objectUrl: string | null;
  loading: boolean;
  error: boolean;
}

@Injectable({ providedIn: 'root' })
export class ExamenImageApercuStore {
  private readonly examenService = inject(ExamenService);

  private readonly entrees = signal<Map<number, ApercuEntree>>(new Map());

  charger(examenId: number, image: ExamenImage): void {
    if (!image.apercuDisponible || this.entrees().has(image.imageId)) {
      return;
    }

    this.patch(image.imageId, { objectUrl: null, loading: true, error: false });

    this.examenService.apercu(examenId, image.imageId).subscribe({
      next: (blob) => {
        const objectUrl = URL.createObjectURL(blob);
        this.patch(image.imageId, { objectUrl, loading: false, error: false });
      },
      error: () => {
        this.patch(image.imageId, { objectUrl: null, loading: false, error: true });
      },
    });
  }

  entree(imageId: number): Signal<ApercuEntree | undefined> {
    return computed(() => this.entrees().get(imageId));
  }

  clearAll(): void {
    for (const entree of this.entrees().values()) {
      if (entree.objectUrl) {
        URL.revokeObjectURL(entree.objectUrl);
      }
    }
    this.entrees.set(new Map());
  }

  private patch(imageId: number, entree: ApercuEntree): void {
    this.entrees.update((map) => new Map(map).set(imageId, entree));
  }
}
