import { Component } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { PublicHeaderComponent } from './shared/public-header.component';
import { AuthenticatedHeaderComponent } from './shared/authenticated-header.component';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, PublicHeaderComponent, AuthenticatedHeaderComponent],
  template: `
    <a class="skip-link" href="#contenido" (click)="saltarAlContenido($event)">Saltar al contenido</a>
    @if (auth.autenticado()) {
      <app-authenticated-header [nombre]="auth.usuario()!.nombre_completo" (cerrarSesion)="auth.cerrarSesion()" />
    } @else { <app-public-header /> }
    <main id="contenido" class="container" tabindex="-1"><router-outlet /></main>
    <footer class="container footer"><strong>Entrevista Pro 360</strong><span>Prepárate con intención, avanza con confianza.</span></footer>
  `
})
export class AppComponent {
  readonly auth = inject(AuthService);
  private readonly document = inject(DOCUMENT);
  saltarAlContenido(event: Event): void {
    event.preventDefault();
    this.document.getElementById('contenido')?.focus();
  }
}
