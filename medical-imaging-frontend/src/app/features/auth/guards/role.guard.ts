import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '../models/auth.model';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const rolesAutorises = route.data['roles'] as Role[] | undefined;
  const role = authService.currentUser()?.role;

  if (!rolesAutorises || (role !== undefined && rolesAutorises.includes(role))) {
    return true;
  }

  return router.createUrlTree(['/']);
};
