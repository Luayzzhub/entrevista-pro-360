import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { BehaviorSubject, catchError, map, of, startWith, switchMap, tap } from 'rxjs';
import { CatalogoService } from '../interviews/catalogo.service';
import { EstadoSesion, FiltrosHistorial, ResumenSesiones, SesionService, estadoSesionTexto } from '../interviews/sesion.service';

interface EstadoHistorial { cargando: boolean; resumen: ResumenSesiones | null; error: string | null; }
@Component({
  selector: 'app-historial', imports: [RouterLink, FormsModule, DatePipe, DecimalPipe],
  templateUrl: './historial.component.html', styleUrl: './historial.component.css'
})
export class HistorialComponent {
  private readonly sesiones = inject(SesionService);
  private readonly catalogo = inject(CatalogoService);
  private readonly solicitudes = new BehaviorSubject<FiltrosHistorial>({ page: 0, size: 10 });
  private readonly historicos = signal<{ id_puesto: number; nombre_puesto: string }[]>([]);
  readonly errorCatalogo = signal(false);
  private readonly catalogoPuestos = toSignal(this.catalogo.listarPuestos().pipe(catchError(() => {
    this.errorCatalogo.set(true); return of([]);
  })), { initialValue: [] });
  readonly puestos = computed(() => {
    const unicos = new Map(this.historicos().map(p => [p.id_puesto, p]));
    for (const p of this.catalogoPuestos()) unicos.set(p.id_puesto, p);
    return [...unicos.values()].sort((a, b) => a.nombre_puesto.localeCompare(b.nombre_puesto));
  });
  puesto = ''; desde = ''; hasta = ''; estadoFiltro: EstadoSesion | '' = ''; rubrica = ''; tamano = 10;
  readonly errorFormulario = signal<string | null>(null);
  readonly estadoTexto = estadoSesionTexto;
  readonly estado = toSignal(this.solicitudes.pipe(switchMap(filtros => this.sesiones.consultarResumen(filtros).pipe(
    tap(r => this.historicos.set(r.puestos_historial)),
    map(resumen => ({ cargando: false, resumen, error: null } as EstadoHistorial)),
    startWith({ cargando: true, resumen: null, error: null } as EstadoHistorial),
    catchError((e: Error) => of({ cargando: false, resumen: null, error: e.message } as EstadoHistorial))
  ))), { requireSync: true });
  readonly puntos = computed(() => {
    const serie = this.estado().resumen?.comparacion.serie ?? [];
    return serie.map((p, i) => ({ ...p, x: serie.length === 1 ? 350 : 55 + i * 590 / (serie.length - 1), y: 220 - p.puntaje_promedio * 1.8 }));
  });
  readonly linea = computed(() => this.puntos().map(p => p.x + ',' + p.y).join(' '));
  aplicar(): void {
    if (this.desde && this.hasta && this.desde > this.hasta) {
      this.errorFormulario.set('La fecha inicial no puede ser posterior a la fecha final.'); return;
    }
    this.errorFormulario.set(null);
    this.solicitudes.next({ id_puesto: this.puesto ? Number(this.puesto) : undefined,
      fecha_desde: this.desde || undefined, fecha_hasta: this.hasta || undefined,
      estado: this.estadoFiltro || undefined, version_rubrica: this.rubrica || undefined, page: 0, size: this.tamano });
  }
  limpiar(): void {
    this.puesto = ''; this.desde = ''; this.hasta = ''; this.estadoFiltro = ''; this.rubrica = ''; this.tamano = 10;
    this.aplicar();
  }
  pagina(delta: number): void {
    const r = this.estado().resumen;
    if (r && r.pagina + delta >= 0 && r.pagina + delta < r.total_paginas)
      this.solicitudes.next({ ...this.solicitudes.value, page: r.pagina + delta });
  }
  reintentar(): void { this.solicitudes.next({ ...this.solicitudes.value }); }
  incompatible(): boolean {
    const f = this.solicitudes.value;
    return f.version_rubrica === 'COM-1.0' && !!f.estado && f.estado !== 'COMPLETADA';
  }
  conSigno(valor: number | null): string {
    return valor === null ? 'Sin datos' : (valor > 0 ? '+' : '') + valor.toFixed(2);
  }
}
