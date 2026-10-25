import { Component, DestroyRef, ElementRef, effect, inject, input, signal, viewChild } from '@angular/core';
import { HttpEventType } from '@angular/common/http';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RespuestaService } from './respuesta.service';
import { ApiClientError } from '../../core/api-error.interceptor';
import { mensajeApi } from '../../shared/form-api-error';
import { DecimalPipe } from '@angular/common';
import { Pregunta } from './catalogo.service';
import { GrabacionMediaService } from './grabacion-media.service';

@Component({
  selector: 'app-captura-respuesta',
  imports: [DecimalPipe],
  providers: [GrabacionMediaService],
  templateUrl: './captura-respuesta.component.html'
})
export class CapturaRespuestaComponent {
  readonly idSesion = input.required<number>();
  readonly subiendo = signal(false);
  readonly porcentajeCarga = signal(0);
  readonly errorCarga = signal<string | null>(null);
  readonly requiereNuevaToma = signal(false);
  private readonly respuestas = inject(RespuestaService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly puesto = input.required<string>();
  readonly pregunta = input.required<Pregunta>();
  readonly totalPreguntas = input.required<number>();
  readonly media = inject(GrabacionMediaService);
  private readonly vistaPrevia = viewChild<ElementRef<HTMLVideoElement>>('vistaPrevia');
  constructor() {
    effect(onCleanup => {
      const video = this.vistaPrevia()?.nativeElement, stream = this.media.stream();
      if (!video) return;
      video.muted = true; video.srcObject = stream;
      if (stream) void video.play().catch(() => { /* El control de reproducción permite reintentar la vista previa. */ });
      onCleanup(() => { video.srcObject = null; });
    });
  }
  reloj(ms: number): string {
    const segundos = Math.max(0, Math.floor(ms / 1000));
    return `${Math.floor(segundos / 60).toString().padStart(2, '0')}:${(segundos % 60).toString().padStart(2, '0')}`;
  }
  segundosRestantes(): number { return Math.max(0, Math.ceil(this.media.limiteSegundos() - this.media.transcurridoMs() / 1000)); }
  repetir(): void {
    if (this.subiendo()) return;
    this.errorCarga.set(null); this.requiereNuevaToma.set(false); this.porcentajeCarga.set(0); this.media.repetir();
  }
  confirmar(): void {
    const toma = this.media.toma();
    if (!toma || toma.errores.length > 0 || this.subiendo() || this.requiereNuevaToma()) return;
    this.subiendo.set(true); this.errorCarga.set(null); this.porcentajeCarga.set(0);
    this.respuestas.confirmarRespuesta(this.idSesion(), this.pregunta().id_pregunta, toma.blob, toma.formato.contenedor)
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: evento => {
          if (evento.type === HttpEventType.UploadProgress) this.porcentajeCarga.set(Math.min(100, Math.floor(100 * evento.loaded / (evento.total || toma.blob.size))));
          if (evento.type === HttpEventType.Response) {
            if (evento.status === 202 && evento.body) {
              this.porcentajeCarga.set(100); this.media.descartarTomaConfirmada();
              void this.router.navigate(['/sesiones', this.idSesion(), 'respuestas', evento.body.id_respuesta]);
            } else { this.subiendo.set(false); this.errorCarga.set('No se pudo confirmar la respuesta. Conservamos tu toma para volver a intentarlo.'); }
          }
        },
        error: error => {
          this.subiendo.set(false); this.errorCarga.set(mensajeApi(error));
          this.requiereNuevaToma.set(error instanceof ApiClientError && [413, 415, 422].includes(error.status));
        }
      });
  }
}
