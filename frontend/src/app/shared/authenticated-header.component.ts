import { Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-authenticated-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="site-header"><div class="container header-inner">
      <a class="brand" routerLink="/panel">Entrevista Pro <b>360</b></a>
      <nav aria-label="Navegación de cuenta">
        <a routerLink="/panel" routerLinkActive="active" ariaCurrentWhenActive="page">Panel</a>
        <a routerLink="/puestos" routerLinkActive="active" ariaCurrentWhenActive="page">Puestos</a>
        <a routerLink="/perfil" routerLinkActive="active" ariaCurrentWhenActive="page">Perfil</a>
        <span class="account-name">{{ nombre() }}</span>
        <button type="button" class="button secondary" (click)="cerrarSesion.emit()">Cerrar sesión</button>
      </nav>
    </div></header>
  `
})
export class AuthenticatedHeaderComponent {
  readonly nombre = input.required<string>();
  readonly cerrarSesion = output<void>();
}
