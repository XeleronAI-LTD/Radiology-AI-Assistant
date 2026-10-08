import { Injectable, Signal, computed, inject, signal } from '@angular/core';
import { Detection } from '../models/detection.model';
import { DetectionService } from './detection.service';

export interface MasqueEntree {
  objectUrl: string | null;
  loading: boolean;
  error: boolean;
}

@Injectable({ providedIn: 'root' })
export class DetectionMasqueStore {
  private readonly detectionService = inject(DetectionService);

  private readonly entrees = signal<Map<number, MasqueEntree>>(new Map());

  charger(examenId: number, detection: Detection): void {
    if (
      detection.type !== 'MASQUE' ||
      !detection.apercuMasqueDisponible ||
      this.entrees().has(detection.id)
    ) {
      return;
    }

    this.patch(detection.id, { objectUrl: null, loading: true, error: false });

    this.detectionService.masque(examenId, detection.id).subscribe({
      next: (blob) => {
        const objectUrl = URL.createObjectURL(blob);
        this.patch(detection.id, { objectUrl, loading: false, error: false });
      },
      error: () => {
        this.patch(detection.id, { objectUrl: null, loading: false, error: true });
      },
    });
  }

  entree(detectionId: number): Signal<MasqueEntree | undefined> {
    return computed(() => this.entrees().get(detectionId));
  }

  clearAll(): void {
    for (const entree of this.entrees().values()) {
      if (entree.objectUrl) {
        URL.revokeObjectURL(entree.objectUrl);
      }
    }
    this.entrees.set(new Map());
  }

  private patch(detectionId: number, entree: MasqueEntree): void {
    this.entrees.update((map) => new Map(map).set(detectionId, entree));
  }
}
