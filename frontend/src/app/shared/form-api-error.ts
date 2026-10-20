import { AbstractControl } from '@angular/forms';
import { ApiClientError } from '../core/api-error.interceptor';

export function mensajeApi(error: unknown): string {
  return error instanceof Error ? error.message : 'No pudimos completar la operación. Inténtalo de nuevo.';
}
export function erroresDeCampo(error: unknown, formulario: AbstractControl): void {
  if (!(error instanceof ApiClientError)) return;
  for (const [nombre, mensaje] of Object.entries(error.detalle.errores_campo ?? {})) {
    const control = formulario.get(nombre);
    if (control) { control.setErrors({ ...control.errors, api: mensaje }); control.markAsTouched(); }
  }
}
