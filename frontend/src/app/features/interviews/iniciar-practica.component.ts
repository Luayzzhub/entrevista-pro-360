import { Component, inject, input } from '@angular/core';
import { ContextoPractica } from './contexto-practica';

@Component({
  selector: 'app-iniciar-practica',
  template: `@if (contexto.auth.autenticado()) { <button class="button start-practice" type="button" [disabled]="contexto.bloqueado()" [attr.aria-label]="'Iniciar práctica de ' + nombrePuesto()" (click)="contexto.iniciar(idPuesto())">{{ contexto.creando() === idPuesto() ? 'Iniciando…' : 'Iniciar práctica' }}</button> }`
})
export class IniciarPracticaComponent {
  readonly contexto = inject(ContextoPractica);
  readonly idPuesto = input.required<number>();
  readonly nombrePuesto = input('este puesto');
}
