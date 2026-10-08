import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../auth/services/auth.service';
import { ExamenSummary } from '../../models/examen.model';
import { ExamenService } from '../../services/examen.service';

@Component({
  selector: 'app-examens-liste',
  imports: [DatePipe],
  templateUrl: './examens-liste.html',
})
export class ExamensListe implements OnInit {
  private readonly examenService = inject(ExamenService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private readonly PAGE_SIZE = 20;

  protected readonly examens = signal<ExamenSummary[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly mrnFilter = signal('');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.fetch(0);
  }

  protected onSearch(mrn: string): void {
    this.mrnFilter.set(mrn.trim());
    this.fetch(0);
  }

  protected onRetry(): void {
    this.fetch(this.page());
  }

  protected onPrevious(): void {
    if (this.page() > 0) {
      this.fetch(this.page() - 1);
    }
  }

  protected onNext(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.fetch(this.page() + 1);
    }
  }

  protected onRowClick(examenId: number): void {
    this.router.navigate(['/examens', examenId]);
  }

  protected readonly peutUploader = computed(() => {
    const role = this.authService.currentUser()?.role;
    return role === 'RADIOLOGUE' || role === 'TECHNICIEN';
  });

  protected onNouvelExamen(): void {
    this.router.navigate(['/examens/nouveau']);
  }

  private fetch(page: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.examenService.lister(this.mrnFilter() || null, page, this.PAGE_SIZE).subscribe({
      next: (response) => {
        this.examens.set(response.content);
        this.page.set(response.page);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Impossible de charger les examens, réessayez plus tard.');
      },
    });
  }
}
