import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, interval, switchMap, takeWhile } from 'rxjs';
import { AuthService } from '../../../auth/services/auth.service';
import { AnalyseStatut, Detection } from '../../models/detection.model';
import { ExamenDetail as ExamenDetailModel } from '../../models/examen.model';
import { DetectionMasqueStore } from '../../services/detection-masque-store.service';
import { DetectionService } from '../../services/detection.service';
import { ExamenImageApercuStore } from '../../services/examen-image-apercu-store.service';
import { ExamenService } from '../../services/examen.service';
import { ExamenImageApercu } from './examen-image-apercu';
import { ExamenImageViewer } from './examen-image-viewer';

const POLL_INTERVAL_MS = 3000;

@Component({
  selector: 'app-examen-detail',
  imports: [DatePipe, ExamenImageApercu, ExamenImageViewer],
  templateUrl: './examen-detail.html',
})
export class ExamenDetail implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly examenService = inject(ExamenService);
  private readonly detectionService = inject(DetectionService);
  private readonly authService = inject(AuthService);
  private readonly apercuStore = inject(ExamenImageApercuStore);
  private readonly detectionMasqueStore = inject(DetectionMasqueStore);

  private readonly examenId: number;
  private pollingSub?: Subscription;

  protected readonly examen = signal<ExamenDetailModel | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<'not-found' | 'generic' | null>(null);
  protected readonly imageOuverteIndex = signal<number | null>(null);
  protected readonly detections = signal<Detection[]>([]);
  protected readonly statutAnalyse = signal<AnalyseStatut | null>(null);
  protected readonly analyseMessage = signal<string | null>(null);
  protected readonly erreurAnalyse = signal<string | null>(null);
  protected readonly lancementEnCours = signal(false);

  protected readonly peutAnalyser = computed(() => {
    const role = this.authService.currentUser()?.role;
    return role === 'RADIOLOGUE' || role === 'TECHNICIEN';
  });

  protected readonly peutValider = computed(() => this.authService.currentUser()?.role === 'RADIOLOGUE');

  protected readonly analyseEnCours = computed(
    () => this.lancementEnCours() || this.statutAnalyse() === 'EN_COURS'
  );

  protected readonly analyseDejaTerminee = computed(() => this.statutAnalyse() === 'TERMINEE');

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.examenId = idParam !== null ? Number(idParam) : NaN;
  }

  ngOnInit(): void {
    if (Number.isNaN(this.examenId)) {
      this.error.set('not-found');
      this.loading.set(false);
      return;
    }
    this.fetch();
  }

  ngOnDestroy(): void {
    this.apercuStore.clearAll();
    this.detectionMasqueStore.clearAll();
    this.pollingSub?.unsubscribe();
  }

  protected onRetry(): void {
    this.fetch();
  }

  protected onRetourListe(): void {
    this.router.navigateByUrl('/');
  }

  protected onOuvrirImage(index: number): void {
    this.imageOuverteIndex.set(index);
  }

  protected onFermerViewer(): void {
    this.imageOuverteIndex.set(null);
  }

  protected onLancerAnalyse(): void {
    this.erreurAnalyse.set(null);
    this.lancementEnCours.set(true);

    this.detectionService.lancerAnalyse(this.examenId).subscribe({
      next: () => {
        this.lancementEnCours.set(false);
        this.statutAnalyse.set('EN_COURS');
        this.analyseMessage.set(null);
        this.demarrerPolling();
      },
      error: (err: HttpErrorResponse) => {
        this.lancementEnCours.set(false);
        this.erreurAnalyse.set(this.messageErreurAnalyse(err));
      },
    });
  }

  protected onValiderDetection(event: { detectionId: number; statut: 'ACCEPTEE' | 'REJETEE' }): void {
    this.detectionService.validerStatut(this.examenId, event.detectionId, event.statut).subscribe({
      next: (detectionMiseAJour) => {
        this.detections.update((liste) =>
          liste.map((d) => (d.id === detectionMiseAJour.id ? detectionMiseAJour : d))
        );
      },
    });
  }

  private fetch(): void {
    this.loading.set(true);
    this.error.set(null);

    this.examenService.detail(this.examenId).subscribe({
      next: (response) => {
        this.examen.set(response);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.status === 404 ? 'not-found' : 'generic');
      },
    });

    this.chargerDetections();

    this.detectionService.statutAnalyse(this.examenId).subscribe({
      next: (reponse) => {
        this.statutAnalyse.set(reponse.statut);
        this.analyseMessage.set(reponse.message);
        if (reponse.statut === 'EN_COURS') {
          this.demarrerPolling();
        }
      },
      error: () => this.statutAnalyse.set(null),
    });
  }

  private chargerDetections(): void {
    this.detectionService.lister(this.examenId).subscribe({
      next: (reponse) => this.detections.set(reponse),
      error: () => this.detections.set([]),
    });
  }

  private demarrerPolling(): void {
    this.pollingSub?.unsubscribe();
    this.pollingSub = interval(POLL_INTERVAL_MS)
      .pipe(
        switchMap(() => this.detectionService.statutAnalyse(this.examenId)),
        takeWhile((reponse) => reponse.statut === 'EN_COURS', true)
      )
      .subscribe({
        next: (reponse) => {
          this.statutAnalyse.set(reponse.statut);
          this.analyseMessage.set(reponse.message);
          if (reponse.statut === 'TERMINEE') {
            this.chargerDetections();
          }
        },
      });
  }

  private messageErreurAnalyse(err: HttpErrorResponse): string {
    if (err.status === 409) {
      return 'Une analyse est déjà en cours pour cet examen.';
    }
    if (err.status === 422) {
      return typeof err.error === 'string'
        ? err.error
        : 'Analyse impossible : modalité/zone manquante ou aucune image.';
    }
    return "Échec du lancement de l'analyse, réessayez plus tard.";
  }
}
