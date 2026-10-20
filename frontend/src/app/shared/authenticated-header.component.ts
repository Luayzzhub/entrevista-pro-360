import { Component, input, output } from '@angular/core';

/** Componente preparado para identidad; no supone que exista una sesión autenticada. */
@Component({
  selector: 'app-authenticated-header',
  template: `
    <header class="site-header"><div class="container header-inner">
      <button type="button" class="brand brand-button" (click)="irAlPanel.emit()">Entrevista Pro <b>360</b></button>
      <nav aria-label="Navegación de cuenta">
        <span>{{ nombre() }}</span>
        <button type="button" class="button secondary" (click)="cerrarSesion.emit()">Cerrar sesión</button>
      </nav>
    </div></header>
  `
})
export class AuthenticatedHeaderComponent {
  readonly nombre = input.required<string>();
  readonly irAlPanel = output<void>();
  readonly cerrarSesion = output<void>();
}
