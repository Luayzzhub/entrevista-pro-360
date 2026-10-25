import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { catchError, map, of, startWith, Subject, switchMap } from 'rxjs';
import { ResumenSesiones, SesionService, estadoSesionTexto } from './sesion.service';
import { CancelarPracticaComponent } from './cancelar-practica.component';

interface EstadoPanel { cargando: boolean; resumen: ResumenSesiones | null; error: string | null; }

@Component({
  selector: 'app-panel', imports: [RouterLink, DecimalPipe, CancelarPracticaComponent],
  template: `
    <section class="dashboard" aria-labelledby="titulo-panel" [attr.aria-busy]="estado().cargando">
      <div class="dashboard-heading"><span class="eyebrow">TU ESPACIO DE PREPARACIÓN</span><h1 id="titulo-panel">Hola, {{ auth.usuario()?.nombre_completo }}.</h1><p>Recibirás feedback después de cada respuesta y un resumen al completar las tres preguntas.</p></div>
      @if (estado().cargando) { <p class="status-panel practice-banner" role="status">Cargando tus prácticas…</p> }
      @else if (estado().error) { <div class="status-panel error practice-banner" role="alert"><p>{{ estado().error }}</p><button type="button" class="button" (click)="recargar.next()">Volver a intentar</button></div> }
      @else if (estado().resumen; as resumen) {
        <div class="dashboard-grid">
          <article class="form-card dashboard-practice">
            @if (resumen.sesion_pendiente; as pendiente) {
              <span class="pill">Práctica en curso</span><h2>{{ pendiente.nombre_puesto }}</h2><p>{{ estadoTexto(pendiente.estado_sesion) }}</p>
              <p class="session-progress">Incompleta: {{ pendiente.respuestas_evaluadas }} de {{ pendiente.total_preguntas }} respuestas evaluadas</p>
              @if (pendiente.orden_pregunta_habilitada) { <p>Pregunta {{ pendiente.orden_pregunta_habilitada }} habilitada para responder.</p> }
              <div class="session-actions"><a class="button" [routerLink]="['/sesiones', pendiente.id_sesion]">Continuar práctica</a><app-cancelar-practica [idSesion]="pendiente.id_sesion" (cancelada)="recargar.next()" /></div>
            } @else {
              <span class="pill">Tu próxima práctica</span><h2>Prepara tu próxima entrevista</h2><p>Elige un puesto y comienza una sesión con sus tres preguntas.</p><a class="button" routerLink="/puestos">Nueva práctica <span aria-hidden="true">↗</span></a>
            }
          </article>
          <article class="form-card"><span class="eyebrow">ÚLTIMA SESIÓN COMPLETADA</span>
            @if (resumen.ultima_sesion_completada; as ultima) {
              <h2>{{ ultima.nombre_puesto }}</h2><p class="session-score">Promedio: {{ ultima.puntaje_promedio | number:'1.2-2' }} / 100</p><p>Sesiones completadas: {{ resumen.total_sesiones_completadas }}</p><a class="button secondary" [routerLink]="['/sesiones', ultima.id_sesion]">Ver sesión</a>
            } @else { <h2>Aún no tienes sesiones completadas</h2><p>Sesiones completadas: {{ resumen.total_sesiones_completadas }}</p> }
          </article>
          <aside class="form-card"><span class="eyebrow">MI CUENTA</span><h2>{{ auth.usuario()?.nombre_completo }}</h2><p class="account-email">{{ auth.usuario()?.email }}</p><p class="account-status">Cuenta activa · Postulante</p><a class="button secondary" routerLink="/perfil">Mi perfil</a></aside>
        </div>
      }
      <p class="availability practice-banner">Puedes grabar, revisar y confirmar tu video de forma privada. El análisis se integrará en los siguientes incrementos.</p>
    </section>
  `
})
export class PanelComponent {
  readonly auth = inject(AuthService);
  private readonly sesiones = inject(SesionService);
  readonly recargar = new Subject<void>();
  readonly estadoTexto = estadoSesionTexto;
  readonly estado = toSignal(this.recargar.pipe(startWith(undefined), switchMap(() => this.sesiones.consultarResumen().pipe(
    map(resumen => ({ cargando: false, resumen, error: null } as EstadoPanel)),
    startWith({ cargando: true, resumen: null, error: null } as EstadoPanel),
    catchError((error: Error) => of({ cargando: false, resumen: null, error: error.message } as EstadoPanel))
  ))), { requireSync: true });
}
