import { Component, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, combineLatest, map, of, startWith, Subject, switchMap } from 'rxjs';
import { PreguntaSesion, SesionDetalle, SesionService, EstadoSesion, estadoSesionTexto } from './sesion.service';
import { CancelarPracticaComponent } from './cancelar-practica.component';
import { CapturaRespuestaComponent } from './captura-respuesta.component';

interface EstadoVista { cargando: boolean; sesion: SesionDetalle | null; error: string | null; }

@Component({
  selector: 'app-sesion', imports: [RouterLink, DatePipe, DecimalPipe, CancelarPracticaComponent, CapturaRespuestaComponent],
  template: `
    <section class="session-page" aria-labelledby="titulo-sesion" [attr.aria-busy]="estado().cargando">
      <a class="text-link back-link" routerLink="/panel">← Volver al panel</a><span class="eyebrow">TU PRÁCTICA DE ENTREVISTA</span><h1 id="titulo-sesion">Sesión de práctica</h1>
      @if (estado().cargando) { <p class="status-panel practice-banner" role="status">Cargando tu sesión…</p> }
      @else if (estado().error) { <div class="status-panel error practice-banner" role="alert"><h2>No pudimos abrir la sesión</h2><p>{{ estado().error }}</p><button class="button" type="button" (click)="recargar.next()">Volver a intentar</button></div> }
      @else if (estado().sesion; as sesion) {
        <header class="form-card session-heading"><span class="pill">{{ estadoTexto(sesion.estado_sesion) }}</span><h2>{{ sesion.nombre_puesto }}</h2><p>{{ sesion.area_puesto }} · {{ sesion.nivel_experiencia }}</p><p class="session-progress">{{ sesion.respuestas_evaluadas }} de {{ sesion.total_preguntas }} respuestas evaluadas</p><p>Respuestas confirmadas: {{ sesion.respuestas_confirmadas }}</p><p class="session-date">Inicio: {{ sesion.fecha_inicio | date:'dd/MM/yyyy HH:mm:ss':'UTC' }} UTC</p>
          @if (sesion.fecha_fin) { <p class="session-date">Fin: {{ sesion.fecha_fin | date:'dd/MM/yyyy HH:mm:ss':'UTC' }} UTC</p> }
          @if (sesion.estado_sesion === 'COMPLETADA') { <p class="session-score">Promedio: {{ sesion.puntaje_promedio | number:'1.2-2' }} / 100</p> }
          @if (sesion.estado_sesion === 'CANCELADA') { <p class="notice privacy">Esta práctica está cancelada. Sus respuestas se conservan y no tiene promedio final.</p> }
        </header>
        @if (!esTerminal(sesion.estado_sesion)) {
          @if (sesion.pregunta_habilitada; as pregunta) {
            <app-captura-respuesta [puesto]="sesion.nombre_puesto" [pregunta]="pregunta" [totalPreguntas]="sesion.total_preguntas" />
          } @else { <p class="status-panel practice-banner" role="status">No hay otra pregunta habilitada. Consulta el estado de tus respuestas; el avance requiere que la anterior esté evaluada.</p> }
        }
        <h2 class="session-list-title">Las tres preguntas de tu práctica</h2>
        <ol class="question-list">
          @for (pregunta of sesion.preguntas; track pregunta.id_pregunta) {
            <li class="question-card" [class.available-question]="pregunta.habilitada"><div class="question-number" aria-hidden="true">0{{ pregunta.orden }}</div><div><span class="tag">{{ estadoPregunta(pregunta, esTerminal(sesion.estado_sesion)) }}</span><h3>{{ pregunta.enunciado }}</h3><p class="competency">{{ pregunta.competencia_evaluada }}</p></div></li>
          }
        </ol>
        <div class="session-actions"><a class="button secondary" routerLink="/panel">Volver al panel</a>
          @if (esTerminal(sesion.estado_sesion)) { <a class="button" routerLink="/puestos">Nueva práctica</a> }
          @else { <app-cancelar-practica [idSesion]="sesion.id_sesion" etiqueta="Finalizar práctica anticipadamente" (cancelada)="recargar.next()" /> }
        </div>
      }
    </section>
  `
})
export class SesionComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly sesiones = inject(SesionService);
  readonly recargar = new Subject<void>();
  readonly estadoTexto = estadoSesionTexto;
  readonly estado = toSignal(combineLatest([this.route.paramMap, this.recargar.pipe(startWith(undefined))]).pipe(
    switchMap(([params]) => {
      const raw = params.get('id') ?? ''; const id = Number(raw);
      if (!/^\d+$/.test(raw) || !Number.isSafeInteger(id) || id < 1) return of({ cargando: false, sesion: null, error: 'El identificador de la sesión no es válido.' } as EstadoVista);
      return this.sesiones.consultarSesion(id).pipe(
        map(sesion => ({ cargando: false, sesion, error: null } as EstadoVista)),
        startWith({ cargando: true, sesion: null, error: null } as EstadoVista),
        catchError((error: Error) => of({ cargando: false, sesion: null, error: error.message } as EstadoVista))
      );
    })
  ), { requireSync: true });
  esTerminal(estado: EstadoSesion): boolean { return estado === 'COMPLETADA' || estado === 'CANCELADA'; }
  estadoPregunta(pregunta: PreguntaSesion, terminal: boolean): string {
    if (pregunta.estado_procesamiento !== null) return { SUBIDO: 'Video subido', AUDIO_EXTRAIDO: 'Audio extraído', TRANSCRITO: 'Transcrita', EVALUADO: 'Evaluada', FALLIDO: 'Procesamiento fallido' }[pregunta.estado_procesamiento];
    if (pregunta.habilitada) return 'Disponible para responder';
    return terminal ? 'Sin respuesta' : 'Bloqueada hasta evaluar la pregunta anterior';
  }
}
