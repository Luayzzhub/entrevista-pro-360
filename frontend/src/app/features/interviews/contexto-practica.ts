import { DestroyRef, Injectable, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ApiClientError } from '../../core/api-error.interceptor';
import { SesionService, SesionResumen } from './sesion.service';
import { mensajeApi } from '../../shared/form-api-error';

/** Estado por vista: una consulta de resumen compartida por aviso y botones. */
@Injectable()
export class ContextoPractica {
  readonly auth = inject(AuthService);
  private readonly sesiones = inject(SesionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly revision = signal(0);
  readonly pendiente = signal<SesionResumen | null>(null);
  readonly cargando = signal(this.auth.autenticado());
  readonly error = signal<string | null>(null);
  readonly creando = signal<number | null>(null);
  readonly errorInicio = signal<string | null>(null);
  readonly idConflicto = signal<number | null>(null);
  constructor() {
    effect(onCleanup => {
      const token = this.auth.accessToken(); this.revision();
      this.pendiente.set(null); this.error.set(null); this.errorInicio.set(null); this.idConflicto.set(null);
      if (token === null) { this.cargando.set(false); return; }
      this.cargando.set(true);
      const solicitud = this.sesiones.consultarResumen().subscribe({
        next: resumen => { this.pendiente.set(resumen.sesion_pendiente); this.cargando.set(false); },
        error: error => { this.error.set(mensajeApi(error)); this.cargando.set(false); }
      });
      onCleanup(() => solicitud.unsubscribe());
    });
  }
  recargar(): void { this.revision.update(n => n + 1); }
  bloqueado(): boolean { return this.cargando() || this.error() !== null || this.pendiente() !== null || this.creando() !== null; }
  iniciar(idPuesto: number): void {
    if (!this.auth.sesionVigente()) { void this.router.navigate(['/login']); return; }
    if (this.bloqueado()) return;
    this.creando.set(idPuesto); this.errorInicio.set(null); this.idConflicto.set(null);
    this.sesiones.crearSesion(idPuesto).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: sesion => { this.creando.set(null); void this.router.navigate(['/sesiones', sesion.id_sesion]); },
      error: error => {
        this.creando.set(null); this.errorInicio.set(mensajeApi(error));
        if (error instanceof ApiClientError && error.detalle.codigo === 'SESION_ACTIVA_EXISTENTE') {
          const id = Number(error.detalle.errores_campo['id_sesion_activa']);
          if (Number.isSafeInteger(id) && id > 0) this.idConflicto.set(id);
        }
      }
    });
  }
}
