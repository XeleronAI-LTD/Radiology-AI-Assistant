import { Routes } from '@angular/router';
import { authGuard } from './features/auth/guards/auth.guard';
import { roleGuard } from './features/auth/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/pages/login/login').then((m) => m.Login),
  },
  {
    path: '',
    loadComponent: () => import('./layout/protected-layout').then((m) => m.ProtectedLayout),
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/examens/pages/liste/examens-liste').then((m) => m.ExamensListe),
      },
      {
        path: 'examens/nouveau',
        loadComponent: () =>
          import('./features/examens/pages/upload/examen-upload').then((m) => m.ExamenUpload),
        canActivate: [roleGuard],
        data: { roles: ['RADIOLOGUE', 'TECHNICIEN'] },
      },
      {
        path: 'examens/:id',
        loadComponent: () =>
          import('./features/examens/pages/detail/examen-detail').then((m) => m.ExamenDetail),
      },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
