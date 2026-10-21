import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, combineLatest, map, of, startWith, Subject, switchMap } from 'rxjs';
import { CatalogoService, Pregunta } from './catalogo.service';
import { ContextoPractica } from './contexto-practica';
import { ContextoPracticaComponent } from './contexto-practica.component';
import { IniciarPracticaComponent } from './iniciar-practica.component';

interface EstadoPreguntas { cargando: boolean; preguntas: Pregunta[]; error: string | null; }

@Component({
  selector: 'app-preguntas',
  imports: [RouterLink, ContextoPracticaComponent, IniciarPracticaComponent],
  providers: [ContextoPractica],
  template: `
    <section class="question-page" aria-labelledby="titulo-preguntas" [attr.aria-busy]="estado().cargando">
      <a class="text-link back-link" routerLink="/puestos">← Volver a los puestos</a>
      <span class="eyebrow">CONOCE TU PRÁCTICA</span><h1 id="titulo-preguntas">Tres preguntas para preparar tus ideas.</h1>
      <p class="intro">Revisa las competencias y piensa en experiencias concretas. Cada respuesta tendrá un máximo de 60 segundos.</p>
      <app-contexto-practica />
      @if (estado().cargando) {
        <div class="status-panel" role="status">Cargando las preguntas…</div>
      } @else if (estado().error) {
        <div class="status-panel error" role="alert"><h2>No pudimos cargar las preguntas</h2><p>{{ estado().error }}</p><button type="button" class="button" (click)="recargar.next()">Volver a intentar</button></div>
      } @else if (estado().preguntas.length === 0) {
        <div class="status-panel" role="status">Este puesto aún no tiene preguntas disponibles.</div>
      } @else {
        <ol class="question-list">
          @for (pregunta of estado().preguntas; track pregunta.id_pregunta) {
            <li class="question-card">
              <div class="question-number" aria-hidden="true">0{{ pregunta.orden }}</div>
              <div><div class="question-meta"><span class="eyebrow">PREGUNTA {{ pregunta.orden }}</span><span class="tag">{{ pregunta.tiempo_limite_seg }} segundos</span></div>
                <h2>{{ pregunta.enunciado }}</h2><p class="competency"><strong>Competencia:</strong> {{ pregunta.competencia_evaluada }}</p>
                @if (pregunta.aplica_star) { <p class="star-note">Para organizar tu ejemplo: situación, tarea, acción y resultado.</p> }
              </div>
            </li>
          }
        </ol>
        <app-iniciar-practica [idPuesto]="estado().preguntas[0].id_puesto" />
      }
      <p class="availability">Esta vista permite conocer las preguntas. La práctica con grabación se habilitará más adelante.</p>
    </section>
  `
})
export class PreguntasComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly catalogo = inject(CatalogoService);
  readonly recargar = new Subject<void>();
  readonly estado = toSignal(combineLatest([this.route.paramMap, this.recargar.pipe(startWith(undefined))]).pipe(
    switchMap(([params]) => {
      const rawId = params.get('id') ?? '';
      const id = Number(rawId);
      if (!/^\d+$/.test(rawId) || !Number.isSafeInteger(id) || id < 1 || id > 2147483647) {
        return of({ cargando: false, preguntas: [], error: 'El identificador del puesto no es válido.' } as EstadoPreguntas);
      }
      return this.catalogo.listarPreguntas(id).pipe(
        map(preguntas => ({ cargando: false, preguntas, error: null } as EstadoPreguntas)),
        startWith({ cargando: true, preguntas: [], error: null } as EstadoPreguntas),
        catchError((error: Error) => of({ cargando: false, preguntas: [], error: error.message } as EstadoPreguntas))
      );
    })
  ), { requireSync: true });
}
