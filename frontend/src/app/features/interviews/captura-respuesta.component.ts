import { Component, ElementRef, effect, inject, input, viewChild } from '@angular/core';
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
}
