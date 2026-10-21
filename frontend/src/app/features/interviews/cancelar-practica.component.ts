import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SesionDetalle, SesionService } from './sesion.service';
import { mensajeApi } from '../../shared/form-api-error';

@Component({
  selector: 'app-cancelar-practica',
  template: `
    @if (!confirmando()) {
      <button type="button" class="button secondary cancel-trigger" (click)="confirmando.set(true)">{{ etiqueta() }}</button>
    } @else {
      <section class="notice privacy cancel-confirmation" role="group" aria-label="Confirmar finalización anticipada">
        <h3>¿Finalizar esta práctica?</h3><p>Se conservarán tus respuestas y la sesión quedará cancelada, sin promedio final. Podrás iniciar una nueva práctica.</p>
        @if (error()) { <p class="field-error" role="alert">{{ error() }}</p> }
        <div class="form-actions"><button type="button" class="button" [disabled]="enviando()" (click)="confirmar()">{{ enviando() ? 'Finalizando…' : 'Sí, finalizar práctica' }}</button><button type="button" class="button secondary" [disabled]="enviando()" (click)="confirmando.set(false)">Seguir practicando</button></div>
      </section>
    }
  `
})
export class CancelarPracticaComponent {
  private readonly sesiones = inject(SesionService);
  private readonly destroyRef = inject(DestroyRef);
  readonly idSesion = input.required<number>();
  readonly etiqueta = input('Finalizar anticipadamente');
  readonly cancelada = output<SesionDetalle>();
  readonly confirmando = signal(false);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  confirmar(): void {
    if (!this.confirmando() || this.enviando()) return;
    this.enviando.set(true); this.error.set(null);
    this.sesiones.cancelarSesion(this.idSesion()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: sesion => { this.enviando.set(false); this.confirmando.set(false); this.cancelada.emit(sesion); },
      error: error => { this.enviando.set(false); this.error.set(mensajeApi(error)); }
    });
  }
}
