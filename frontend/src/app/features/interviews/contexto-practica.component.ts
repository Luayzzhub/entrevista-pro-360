import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContextoPractica } from './contexto-practica';
import { CancelarPracticaComponent } from './cancelar-practica.component';

@Component({
  selector: 'app-contexto-practica', imports: [RouterLink, CancelarPracticaComponent],
  template: `
    @if (!contexto.auth.autenticado()) {
      <aside class="practice-banner"><p>Explora los puestos y sus preguntas. Accede a tu cuenta cuando quieras iniciar una práctica.</p><a class="button" routerLink="/login">Iniciar sesión para practicar</a></aside>
    } @else if (contexto.cargando()) {
      <p class="status-panel practice-banner" role="status">Consultando si tienes una práctica en curso…</p>
    } @else if (contexto.error()) {
      <div class="status-panel error practice-banner" role="alert"><p>{{ contexto.error() }}</p><button type="button" class="button" (click)="contexto.recargar()">Volver a intentar</button></div>
    } @else if (contexto.pendiente(); as pendiente) {
      <aside class="practice-banner pending-practice"><h2>Ya tienes una práctica en curso</h2><p><strong>{{ pendiente.nombre_puesto }}</strong></p><p>Incompleta: {{ pendiente.respuestas_evaluadas }} de {{ pendiente.total_preguntas }} respuestas evaluadas</p><div class="session-actions"><a class="button" [routerLink]="['/sesiones', pendiente.id_sesion]">Continuar práctica</a><app-cancelar-practica [idSesion]="pendiente.id_sesion" etiqueta="Finalizar práctica actual" (cancelada)="contexto.recargar()" /></div></aside>
    }
    @if (contexto.errorInicio()) {
      <div class="notice error" role="alert"><p>{{ contexto.errorInicio() }}</p>@if (contexto.idConflicto(); as id) { <a class="text-link" [routerLink]="['/sesiones', id]">Ir a la sesión activa</a> }</div>
    }
  `
})
export class ContextoPracticaComponent { readonly contexto = inject(ContextoPractica); }
