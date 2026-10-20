import { Routes } from '@angular/router';
import { authGuard, publicOnlyGuard } from './core/auth.guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'puestos' },
  { path: 'login', title: 'Iniciar sesión | Entrevista Pro 360', canActivate: [publicOnlyGuard], loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
  { path: 'registro', title: 'Crear cuenta | Entrevista Pro 360', canActivate: [publicOnlyGuard], loadComponent: () => import('./features/auth/registro.component').then(m => m.RegistroComponent) },
  { path: 'panel', title: 'Mi panel | Entrevista Pro 360', canActivate: [authGuard], loadComponent: () => import('./features/interviews/panel.component').then(m => m.PanelComponent) },
  { path: 'perfil', title: 'Mi perfil | Entrevista Pro 360', canActivate: [authGuard], loadComponent: () => import('./features/profile/perfil.component').then(m => m.PerfilComponent) },
  {
    path: 'puestos', title: 'Puestos | Entrevista Pro 360',
    loadComponent: () => import('./features/interviews/catalogo.component').then(m => m.CatalogoComponent)
  },
  {
    path: 'puestos/:id/preguntas', title: 'Preguntas | Entrevista Pro 360',
    loadComponent: () => import('./features/interviews/preguntas.component').then(m => m.PreguntasComponent)
  },
  { path: '**', redirectTo: 'puestos' }
];
