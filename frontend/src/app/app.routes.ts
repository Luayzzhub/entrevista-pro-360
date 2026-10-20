import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'puestos' },
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
