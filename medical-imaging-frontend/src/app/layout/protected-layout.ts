import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../features/auth/services/auth.service';

@Component({
  selector: 'app-protected-layout',
  imports: [RouterOutlet],
  templateUrl: './protected-layout.html',
})
export class ProtectedLayout {
  protected readonly authService = inject(AuthService);

  protected onLogout(): void {
    this.authService.logout();
  }
}
