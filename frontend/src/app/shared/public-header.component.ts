import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-public-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="site-header">
      <div class="container header-inner">
        <a class="brand" routerLink="/puestos" aria-label="Entrevista Pro 360, inicio">
          <span class="brand-mark" aria-hidden="true">ep</span><span>Entrevista Pro <b>360</b></span>
        </a>
        <nav aria-label="Navegación pública">
          <a routerLink="/puestos" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" ariaCurrentWhenActive="page">Puestos</a>
          <a routerLink="/login" routerLinkActive="active" ariaCurrentWhenActive="page">Iniciar sesión</a>
          <a class="nav-register" routerLink="/registro" routerLinkActive="active" ariaCurrentWhenActive="page">Crear cuenta</a>
        </nav>
      </div>
    </header>
  `
})
export class PublicHeaderComponent { }
