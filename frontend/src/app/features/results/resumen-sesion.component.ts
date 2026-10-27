import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subject, catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';
import { EstadoProcesamiento, EstadoSesion, SesionDetalle, SesionService, estadoSesionTexto } from '../interviews/sesion.service';
import { CancelarPracticaComponent } from '../interviews/cancelar-practica.component';
interface EstadoResumen { cargando: boolean; sesion: SesionDetalle | null; error: string | null; }
@Component({
  selector: 'app-resumen-sesion', imports: [RouterLink, DatePipe, DecimalPipe, CancelarPracticaComponent],
  templateUrl: './resumen-sesion.component.html'
})
export class ResumenSesionComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sesiones = inject(SesionService);
  readonly recargar = new Subject<void>();
  readonly confirmandoNueva = signal(false);
  readonly estadoTexto = estadoSesionTexto;
  readonly estado = toSignal(combineLatest([this.route.paramMap, this.recargar.pipe(startWith(undefined))]).pipe(
    switchMap(([params]) => {
      this.confirmandoNueva.set(false);
      const raw = params.get('id') ?? '', id = Number(raw);
      if (!/^\d+$/.test(raw) || !Number.isSafeInteger(id) || id < 1)
        return of({ cargando: false, sesion: null, error: 'El identificador de la sesión debe ser un entero positivo seguro.' } as EstadoResumen);
      return this.sesiones.consultarSesion(id).pipe(
        map(sesion => ({ cargando: false, sesion, error: null } as EstadoResumen)),
        startWith({ cargando: true, sesion: null, error: null } as EstadoResumen),
        catchError((error: Error) => of({ cargando: false, sesion: null, error: error.message } as EstadoResumen))
      );
    })
  ), { requireSync: true });
  esTerminal(estado: EstadoSesion): boolean { return estado === 'COMPLETADA' || estado === 'CANCELADA'; }
  estadoRespuesta(estado: EstadoProcesamiento | null): string {
    return estado === null ? 'Sin respuesta registrada' : {
      SUBIDO: 'Video subido', AUDIO_EXTRAIDO: 'Audio extraído', TRANSCRITO: 'Transcrita',
      EVALUADO: 'Evaluada', FALLIDO: 'Procesamiento fallido'
    }[estado];
  }
  cancelada(): void { void this.router.navigate(['/puestos']); }
}

