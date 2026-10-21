import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { FormatoGrabacion, MAXIMO_BYTES_TOMA, MENSAJE_INCOMPATIBLE, seleccionarFormato, validarTomaLocal } from './formatos-media';

export interface TomaLocal {
  readonly blob: Blob;
  readonly url: string;
  readonly formato: FormatoGrabacion;
  readonly duracionSegundos: number;
  readonly errores: readonly string[];
}
type EtapaCaptura = 'preparacion' | 'grabando' | 'finalizando' | 'revision';
type MotivoDetencion = 'manual' | 'limite' | 'tamano';

/** Instancia por componente: medios y toma exclusivamente locales, sin cliente HTTP. */
@Injectable()
export class GrabacionMediaService {
  readonly etapa = signal<EtapaCaptura>('preparacion');
  readonly camara = signal<'Pendiente' | 'Lista' | 'Sin acceso'>('Pendiente');
  readonly microfono = signal<'Pendiente' | 'Listo' | 'Sin acceso'>('Pendiente');
  readonly solicitando = signal(false);
  readonly incompatible = signal(false);
  readonly formato = signal<FormatoGrabacion | null>(null);
  readonly stream = signal<MediaStream | null>(null);
  readonly error = signal<string | null>(null);
  readonly toma = signal<TomaLocal | null>(null);
  readonly transcurridoMs = signal(0);
  readonly limiteSegundos = signal(60);
  readonly limiteAlcanzado = signal(false);
  readonly puedeGrabar = computed(() => this.etapa() === 'preparacion' && !this.solicitando() && !this.incompatible()
    && this.camara() === 'Lista' && this.microfono() === 'Listo' && this.formato() !== null);
  private recorder: MediaRecorder | null = null;
  private fragmentos: Blob[] = [];
  private bytesAcumulados = 0;
  private inicioMs = 0;
  private duracionSegundos = 0;
  private motivo: MotivoDetencion | null = null;
  private temporizador: ReturnType<typeof setTimeout> | null = null;
  private revisionPermisos = 0;
  private destruido = false;
  private oyentes: Array<() => void> = [];

  constructor() {
    this.comprobarCompatibilidad();
    inject(DestroyRef).onDestroy(() => this.destruir());
  }

  private comprobarCompatibilidad(): boolean {
    let formato: FormatoGrabacion | null = null;
    if (typeof navigator.mediaDevices?.getUserMedia === 'function' && typeof MediaRecorder !== 'undefined' && typeof MediaRecorder.isTypeSupported === 'function') {
      try { formato = seleccionarFormato(mime => MediaRecorder.isTypeSupported(mime)); } catch { /* Capacidad no disponible. */ }
    }
    this.formato.set(formato); this.incompatible.set(formato === null);
    if (!formato) this.error.set(MENSAJE_INCOMPATIBLE);
    return formato !== null;
  }

  async preparar(): Promise<void> {
    if (this.destruido || this.solicitando() || this.etapa() !== 'preparacion') return;
    this.error.set(null);
    if (!this.comprobarCompatibilidad()) return;
    this.liberarDispositivos();
    const revision = ++this.revisionPermisos;
    this.solicitando.set(true); this.camara.set('Pendiente'); this.microfono.set('Pendiente');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
      // Una solicitud de permisos no se puede abortar; una resolución tardía también debe apagar los dispositivos.
      if (this.destruido || revision !== this.revisionPermisos) { stream.getTracks().forEach(track => track.stop()); return; }
      this.stream.set(stream);
      if (!this.pistasActivas(stream)) { this.fallar('No se recibieron una cámara y un micrófono activos. Conecta ambos dispositivos y vuelve a intentar los permisos.'); return; }
      for (const track of stream.getTracks()) {
        const terminado = () => this.fallar('Se desconectó la cámara o el micrófono. La toma incompleta se descartó. Comprueba los dispositivos y vuelve a intentar los permisos.');
        track.addEventListener('ended', terminado);
        this.oyentes.push(() => track.removeEventListener('ended', terminado));
      }
      this.camara.set('Lista'); this.microfono.set('Listo');
    } catch (error: unknown) {
      if (!this.destruido && revision === this.revisionPermisos) this.fallar(this.mensajePermisos(error));
    } finally {
      if (!this.destruido && revision === this.revisionPermisos) this.solicitando.set(false);
    }
  }

  iniciar(limiteSegundos: number): void {
    if (this.destruido || !this.puedeGrabar()) return;
    const stream = this.stream(), formato = this.formato();
    if (!stream || !formato || !this.pistasActivas(stream)) { this.fallar('La cámara o el micrófono ya no están activos. Vuelve a intentar los permisos.'); return; }
    if (!Number.isInteger(limiteSegundos) || limiteSegundos < 1 || limiteSegundos > 60) { this.fallar('El límite de la pregunta no es válido. Vuelve a consultar la sesión.'); return; }
    this.error.set(null); this.limiteAlcanzado.set(false); this.transcurridoMs.set(0); this.limiteSegundos.set(limiteSegundos);
    this.fragmentos = []; this.bytesAcumulados = 0; this.motivo = null;
    try {
      const recorder = new MediaRecorder(stream, { mimeType: formato.mimeType });
      this.recorder = recorder;
      recorder.ondataavailable = event => {
        if (this.recorder !== recorder || this.destruido) return;
        if (event.data.size > 0) { this.fragmentos.push(event.data); this.bytesAcumulados += event.data.size; }
        if (this.bytesAcumulados >= MAXIMO_BYTES_TOMA && recorder.state === 'recording') this.detener('tamano');
      };
      recorder.onerror = () => this.fallar('Ocurrió un fallo al grabar. La toma incompleta se descartó. Vuelve a preparar la cámara y el micrófono.');
      recorder.onstop = () => {
        if (this.recorder !== recorder || this.destruido) return;
        if (this.motivo === null) { this.fallar('La grabación se interrumpió. La toma incompleta se descartó. Vuelve a intentar los permisos.'); return; }
        this.consolidar(formato);
      };
      this.inicioMs = performance.now();
      recorder.start(250);
      this.etapa.set('grabando'); this.actualizarTiempo();
    } catch {
      this.fallar('No se pudo iniciar la grabación con el formato seleccionado. Revisa los dispositivos y vuelve a intentar los permisos.');
    }
  }

  detener(motivo: MotivoDetencion = 'manual'): void {
    const recorder = this.recorder;
    if (this.etapa() !== 'grabando' || !recorder || recorder.state !== 'recording') return;
    const elapsed = Math.max(0, performance.now() - this.inicioMs);
    const limiteMs = this.limiteSegundos() * 1000;
    this.motivo = elapsed >= limiteMs ? 'limite' : motivo;
    // Duración del intervalo de captura solicitado. El contenedor se verificará con ffprobe en servidor (Incremento 5).
    this.duracionSegundos = (this.motivo === 'limite' ? limiteMs : elapsed) / 1000;
    this.transcurridoMs.set(this.duracionSegundos * 1000); this.limiteAlcanzado.set(this.motivo === 'limite');
    this.limpiarTemporizador(); this.etapa.set('finalizando');
    try { recorder.stop(); } catch { this.fallar('No se pudo finalizar la grabación. La toma incompleta se descartó. Vuelve a intentar los permisos.'); }
  }

  repetir(): void {
    if (this.etapa() !== 'revision') return;
    this.revocarToma(); this.liberarDispositivos(); this.error.set(null); this.transcurridoMs.set(0);
    this.limiteAlcanzado.set(false); this.camara.set('Pendiente'); this.microfono.set('Pendiente'); this.etapa.set('preparacion');
  }

  private actualizarTiempo(): void {
    if (this.destruido || this.etapa() !== 'grabando') return;
    const elapsed = Math.max(0, performance.now() - this.inicioMs);
    const restante = this.limiteSegundos() * 1000 - elapsed;
    this.transcurridoMs.set(Math.min(elapsed, this.limiteSegundos() * 1000));
    const stream = this.stream();
    if (!stream || !this.pistasActivas(stream)) { this.fallar('Se perdió una pista de cámara o micrófono. La toma incompleta se descartó. Vuelve a intentar los permisos.'); return; }
    if (restante <= 0) { this.detener('limite'); return; }
    this.temporizador = setTimeout(() => this.actualizarTiempo(), Math.min(100, restante));
  }

  private consolidar(formato: FormatoGrabacion): void {
    this.limpiarTemporizador(); this.desconectarRecorder();
    const blob = new Blob(this.fragmentos, { type: formato.mimeType });
    this.fragmentos = []; this.bytesAcumulados = 0;
    this.liberarDispositivos(); this.revocarToma();
    try {
      this.toma.set({ blob, url: URL.createObjectURL(blob), formato, duracionSegundos: this.duracionSegundos,
        errores: validarTomaLocal(blob.size, this.duracionSegundos) });
      this.etapa.set('revision');
    } catch { this.fallar('No se pudo preparar la reproducción local. Vuelve a grabar la respuesta.'); }
  }

  private pistasActivas(stream: MediaStream): boolean {
    return stream.getVideoTracks().some(track => track.readyState === 'live' && track.enabled)
      && stream.getAudioTracks().some(track => track.readyState === 'live' && track.enabled);
  }
  private mensajePermisos(error: unknown): string {
    const nombre = error instanceof DOMException || error instanceof Error ? error.name : '';
    if (nombre === 'NotAllowedError' || nombre === 'PermissionDeniedError') return 'No se autorizó el acceso a la cámara o al micrófono. Permite ambos en tu navegador y vuelve a intentar los permisos.';
    if (nombre === 'NotFoundError' || nombre === 'DevicesNotFoundError') return 'No se encontró una cámara o un micrófono. Conecta ambos dispositivos y vuelve a intentar los permisos.';
    if (nombre === 'NotReadableError') return 'La cámara o el micrófono están ocupados o no se pueden leer. Cierra otras aplicaciones que los usen y vuelve a intentar los permisos.';
    return 'No se pudo acceder a la cámara y al micrófono. Revisa su conexión y los permisos del navegador y vuelve a intentarlo.';
  }
  private fallar(mensaje: string): void {
    this.limpiarTemporizador(); this.descartarRecorder(); this.liberarDispositivos(); this.revocarToma();
    this.fragmentos = []; this.bytesAcumulados = 0; this.solicitando.set(false);
    this.camara.set('Sin acceso'); this.microfono.set('Sin acceso'); this.error.set(mensaje); this.etapa.set('preparacion');
  }
  private limpiarTemporizador(): void { if (this.temporizador !== null) clearTimeout(this.temporizador); this.temporizador = null; }
  private desconectarRecorder(): MediaRecorder | null {
    const recorder = this.recorder; this.recorder = null;
    if (recorder) { recorder.ondataavailable = null; recorder.onerror = null; recorder.onstop = null; }
    return recorder;
  }
  private descartarRecorder(): void {
    const recorder = this.desconectarRecorder();
    if (recorder && recorder.state !== 'inactive') { try { recorder.stop(); } catch { /* Continuar liberando recursos. */ } }
  }
  private liberarDispositivos(): void {
    this.oyentes.forEach(quitar => quitar()); this.oyentes = [];
    this.stream()?.getTracks().forEach(track => track.stop()); this.stream.set(null);
    this.camara.set('Pendiente'); this.microfono.set('Pendiente');
  }
  private revocarToma(): void { const toma = this.toma(); if (toma) URL.revokeObjectURL(toma.url); this.toma.set(null); }
  private destruir(): void {
    this.destruido = true; ++this.revisionPermisos; this.limpiarTemporizador(); this.descartarRecorder();
    this.fragmentos = []; this.bytesAcumulados = 0; this.liberarDispositivos(); this.revocarToma();
  }
}
