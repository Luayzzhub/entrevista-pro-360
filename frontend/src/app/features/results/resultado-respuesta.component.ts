import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, KeyValuePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { ClaveCriterio, ClaveStar, RespuestaEstado, RespuestaService } from '../interviews/respuesta.service';
import { SesionDetalle } from '../interviews/sesion.service';
import { CancelarPracticaComponent } from '../interviews/cancelar-practica.component';
import { mensajeApi } from '../../shared/form-api-error';
import { ApiClientError } from '../../core/api-error.interceptor';
@Component({
  selector: 'app-resultado-respuesta', imports: [RouterLink, DatePipe, DecimalPipe, KeyValuePipe, CancelarPracticaComponent],
  templateUrl: './resultado-respuesta.component.html'
})
export class ResultadoRespuestaComponent {
  private readonly servicio = inject(RespuestaService);
  private readonly router = inject(Router);
  private readonly destroy = inject(DestroyRef);
  private readonly params = toSignal(inject(ActivatedRoute).paramMap, { requireSync: true });
  readonly respuesta = signal<RespuestaEstado | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly videoUrl = signal<string | null>(null);
  readonly expiraEn = signal<string | null>(null);
  readonly errorVideo = signal<string | null>(null);
  readonly videoNoDisponible = signal(false);
  readonly renovando = signal(false);
  readonly nuevaPractica = signal(false);
  readonly criterios: { clave: ClaveCriterio; nombre: string }[] = [
    { clave: 'relevancia', nombre: 'Relevancia' }, { clave: 'claridad', nombre: 'Claridad' },
    { clave: 'estructura', nombre: 'Estructura' }, { clave: 'evidencias', nombre: 'Evidencias' }
  ];
  readonly star: { clave: ClaveStar; nombre: string }[] = [
    { clave: 'situacion', nombre: 'Situación' }, { clave: 'tarea', nombre: 'Tarea' },
    { clave: 'accion', nombre: 'Acción' }, { clave: 'resultado', nombre: 'Resultado' }
  ];
  private temporizador: ReturnType<typeof setTimeout> | null = null;
  private videoSolicitud: Subscription | null = null;
  private generacion = 0;
  constructor() {
    effect(onCleanup => {
      const params = this.params(), sid = this.id(params.get('idSesion')), rid = this.id(params.get('idRespuesta'));
      this.respuesta.set(null); this.cargando.set(true); this.error.set(null); this.nuevaPractica.set(false);
      this.limpiarVideo(); let activa = true;
      if (sid === null || rid === null) { this.error.set('Los identificadores de sesión y respuesta deben ser enteros positivos.'); this.cargando.set(false); return; }
      const solicitud = this.servicio.consultarRespuesta(rid).subscribe({
        next: r => {
          if (!activa) return;
          this.cargando.set(false);
          if (r.id_sesion !== sid) { this.error.set('Esta respuesta no pertenece a la sesión indicada.'); return; }
          this.respuesta.set(r);
          if (r.estado_procesamiento === 'EVALUADO' && r.evaluacion) this.renovarVideo();
          else this.error.set('El resultado todavía no está disponible. Consulta el procesamiento de tu respuesta.');
        },
        error: e => { if (activa) { this.cargando.set(false); this.error.set(mensajeApi(e)); } }
      });
      onCleanup(() => { activa = false; solicitud.unsubscribe(); this.limpiarVideo(); });
    });
  }
  renovarVideo(): void {
    const r = this.respuesta(); if (!r || this.renovando()) return;
    this.videoSolicitud?.unsubscribe(); this.cancelarTemporizador();
    const generacion = ++this.generacion; this.renovando.set(true); this.errorVideo.set(null);
    this.videoSolicitud = this.servicio.reproducirRespuesta(r.id_respuesta).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: data => {
        if (generacion !== this.generacion) return;
        this.renovando.set(false);
        try {
          const url = new URL(data.video_url_cloud), expira = Date.parse(data.expira_en);
          if (data.id_respuesta !== r.id_respuesta || !['http:', 'https:'].includes(url.protocol) || !Number.isFinite(expira)) throw Error();
          this.videoUrl.set(url.href); this.expiraEn.set(data.expira_en); this.videoNoDisponible.set(false);
          this.temporizador = setTimeout(() => this.renovarVideo(), Math.min(2147483647, Math.max(1000, expira - Date.now() + 25)));
        } catch { this.videoUrl.set(null); this.errorVideo.set('No pudimos abrir el enlace del video. Puedes renovarlo.'); }
      },
      error: e => {
        if (generacion !== this.generacion) return;
        this.renovando.set(false);
        if (e instanceof ApiClientError && e.status === 404 && e.detalle.codigo === 'VIDEO_NO_DISPONIBLE') {
          this.videoUrl.set(null); this.expiraEn.set(null); this.videoNoDisponible.set(true); this.errorVideo.set('Video no disponible');
        } else this.errorVideo.set(mensajeApi(e));
      }
    });
  }
  falloVideo(): void {
    const expira = this.expiraEn();
    if (expira && Date.now() >= Date.parse(expira)) this.renovarVideo();
    else this.errorVideo.set('No se pudo reproducir el video. Prueba renovar el enlace.');
  }
  cancelada(sesion: SesionDetalle): void {
    const r = this.respuesta(); if (r) this.respuesta.set({ ...r, estado_sesion: sesion.estado_sesion });
    if (this.nuevaPractica()) void this.router.navigate(['/puestos']);
  }
  private id(raw: string | null): number | null { const n = Number(raw); return raw !== null && /^\d+$/.test(raw) && Number.isSafeInteger(n) && n > 0 ? n : null; }
  private cancelarTemporizador(): void { if (this.temporizador !== null) clearTimeout(this.temporizador); this.temporizador = null; }
  private limpiarVideo(): void { this.generacion++; this.videoSolicitud?.unsubscribe(); this.videoSolicitud = null; this.cancelarTemporizador(); this.videoUrl.set(null); this.expiraEn.set(null); this.errorVideo.set(null); this.renovando.set(false); this.videoNoDisponible.set(false); }
}

