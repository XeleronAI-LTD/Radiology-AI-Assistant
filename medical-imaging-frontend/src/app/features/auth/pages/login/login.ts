import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected onSubmit(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl('/');
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);

        // 429 (rate-limiting sur /auth/login) : ce n'est pas un problème
        // d'identifiants, on ne vide donc pas le mot de passe et on affiche
        // le message dédié plutôt que le message générique de panne serveur.
        if (err.status === 429) {
          this.errorMessage.set('Trop de tentatives, réessayez dans une minute.');
          return;
        }

        this.form.patchValue({ password: '' });
        this.errorMessage.set(
          err.status === 401
            ? 'Email ou mot de passe incorrect'
            : 'Impossible de contacter le serveur, réessayez plus tard.'
        );
      },
    });
  }
}
