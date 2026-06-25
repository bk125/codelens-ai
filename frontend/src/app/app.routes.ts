import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'review', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'verify',
    loadComponent: () => import('./features/auth/verify/verify.component').then(m => m.VerifyComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: 'review',
    canActivate: [authGuard],
    loadComponent: () => import('./features/review/review-page/review-page.component').then(m => m.ReviewPageComponent)
  },
  {
    path: 'history',
    canActivate: [authGuard],
    loadComponent: () => import('./features/history/history-page/history-page.component').then(m => m.HistoryPageComponent)
  },
  {
    path: 'explainer',
    canActivate: [authGuard],
    loadComponent: () => import('./features/explainer/explainer.component').then(m => m.ExplainerComponent)
  },
  {
    path: 'migration',
    canActivate: [authGuard],
    loadComponent: () => import('./features/migration/migration.component').then(m => m.MigrationComponent)
  },
  {
    path: 'security-scan',
    canActivate: [authGuard],
    loadComponent: () => import('./features/security-scan/security-scan.component').then(m => m.SecurityScanComponent)
  },
  { path: '**', redirectTo: 'review' }
];
