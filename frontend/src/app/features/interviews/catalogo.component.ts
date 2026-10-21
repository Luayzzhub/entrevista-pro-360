import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, startWith, Subject, switchMap } from 'rxjs';
import { CatalogoService, Puesto } from './catalogo.service';
import { ContextoPractica } from './contexto-practica';
import { ContextoPracticaComponent } from './contexto-practica.component';
import { IniciarPracticaComponent } from './iniciar-practica.component';

interface EstadoCatalogo { cargando: boolean; puestos: Puesto[]; error: string | null; }

@Component({
  selector: 'app-catalogo',
  imports: [RouterLink, ContextoPracticaComponent, IniciarPracticaComponent],
  providers: [ContextoPractica],
  templateUrl: './catalogo.component.html'
})
export class CatalogoComponent {
  private readonly catalogo = inject(CatalogoService);
  readonly recargar = new Subject<void>();
  readonly estado = toSignal(this.recargar.pipe(
    startWith(undefined),
    switchMap(() => this.catalogo.listarPuestos().pipe(
      map(puestos => ({ cargando: false, puestos, error: null } as EstadoCatalogo)),
      startWith({ cargando: true, puestos: [], error: null } as EstadoCatalogo),
      catchError((error: Error) => of({ cargando: false, puestos: [], error: error.message } as EstadoCatalogo))
    ))
  ), { requireSync: true });
}
