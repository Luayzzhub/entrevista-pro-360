import { Routes } from '@angular/router';
import { authGuard, publicOnlyGuard } from './core/auth.guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'puestos' },
  { path: 'login', title: 'Iniciar sesión | Entrevista Pro 360', canActivate: [publicOnlyGuard], loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
  { path: 'registro', title: 'Crear cuenta | Entrevista Pro 360', canActivate: [publicOnlyGuard], loadComponent: () => import('./features/auth/registro.component').then(m => m.RegistroComponent) },
  { path: 'panel', title: 'Mi panel | Entrevista Pro 360', canActivate: [authGuard], loadComponent: () => import('./features/interviews/panel.component').then(m => m.PanelComponent) },
  { path: 'perfil', title: 'Mi perfil | Entrevista Pro 360', canActivate: [authGuard], loadComponent: () => import('./features/profile/perfil.component').then(m => m.PerfilComponent) },
  { path: 'sesiones/:idSesion/respuestas/:idRespuesta/resultado', title: 'Resultado de tu respuesta | Entrevista Pro 360', canActivate: [authGuard], loadComponent: () => import('./features/results/resultado-respuesta.component').then(m => m.ResultadoRespuestaComponent) },
  { path: 'sesiones/:idSesion/respuestas/:idRespuesta', title: 'Procesamiento de respuesta | Entrevista Pro 360', canActivate: [authGuard], loadComponent: () => import('./features/interviews/procesamiento-respuesta.component').then(m => m.ProcesamientoRespuestaComponent) },
  { path: 'sesiones/:id/resumen', title: 'Resumen de sesión | Entrevista Pro 360', canActivate: [authGuard], loadComponent: () => import('./features/results/resumen-sesion.component').then(m => m.ResumenSesionComponent) },
  { path: 'sesiones/:id', title: 'Práctica de entrevista | Entrevista Pro 360', canActivate: [authGuard], loadComponent: () => import('./features/interviews/sesion.component').then(m => m.SesionComponent) },
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
