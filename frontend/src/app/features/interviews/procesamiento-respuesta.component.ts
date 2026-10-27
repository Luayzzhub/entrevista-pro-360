import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { RespuestaEstado, RespuestaService } from './respuesta.service';
import { mensajeApi } from '../../shared/form-api-error';

@Component({
  selector: 'app-procesamiento-respuesta', imports: [RouterLink, DatePipe, DecimalPipe],
  templateUrl: './procesamiento-respuesta.component.html'
})
export class ProcesamientoRespuestaComponent {
  private readonly servicio = inject(RespuestaService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly params = toSignal(inject(ActivatedRoute).paramMap, { requireSync: true });
  private readonly revision = signal(0);
  readonly idSesion = signal(0);
  readonly respuesta = signal<RespuestaEstado | null>(null);
  readonly cargando = signal(true);
  readonly reintentando = signal(false);
  readonly error = signal<string | null>(null);
  constructor() {
    effect(onCleanup => {
      const params = this.params(); this.revision();
      const sesion = this.identificador(params.get('idSesion')), id = this.identificador(params.get('idRespuesta'));
      this.idSesion.set(sesion ?? 0); this.respuesta.set(null); this.error.set(null);
      if (sesion === null || id === null) { this.error.set('Los identificadores de sesión y respuesta deben ser enteros positivos.'); this.cargando.set(false); return; }
      let solicitud: Subscription | null = null, temporizador: ReturnType<typeof setTimeout> | null = null, activa = true;
      const consultar = () => {
        this.cargando.set(true);
        solicitud = this.servicio.consultarRespuesta(id).subscribe({
          next: respuesta => {
            if (!activa) return;
            this.cargando.set(false);
            if (respuesta.id_sesion !== sesion) { this.error.set('Esta respuesta no pertenece a la sesión indicada.'); return; }
            this.respuesta.set(respuesta); this.error.set(null);
            if (['SUBIDO', 'AUDIO_EXTRAIDO', 'TRANSCRITO'].includes(respuesta.estado_procesamiento)
                && !['CANCELADA', 'COMPLETADA'].includes(respuesta.estado_sesion))
              temporizador = setTimeout(consultar, 3000);
          },
          error: error => { if (activa) { this.cargando.set(false); this.error.set(mensajeApi(error)); } }
        });
      };
      consultar();
      onCleanup(() => { activa = false; solicitud?.unsubscribe(); if (temporizador !== null) clearTimeout(temporizador); });
    });
  }
  recargar(): void { this.revision.update(n => n + 1); }
  reintentar(): void {
    const r = this.respuesta();
    if (!r?.puede_reintentar || this.reintentando()) return;
    this.reintentando.set(true); this.error.set(null);
    this.servicio.reintentarRespuesta(r.id_respuesta).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: actualizado => { this.respuesta.set(actualizado); this.reintentando.set(false); this.recargar(); },
      error: error => { this.reintentando.set(false); this.error.set(mensajeApi(error)); }
    });
  }
  private identificador(raw: string | null): number | null { const n = Number(raw); return raw !== null && /^\d+$/.test(raw) && Number.isSafeInteger(n) && n > 0 ? n : null; }
}
