import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export interface ApiError {
  codigo: string;
  mensaje: string;
  errores_campo: Record<string, string>;
}

export class ApiClientError extends Error {
  constructor(readonly status: number, readonly detalle: ApiError) { super(detalle.mensaje); }
}

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => next(request).pipe(
  catchError((error: unknown) => {
    if (!(error instanceof HttpErrorResponse)) return throwError(() => error);
    const body: unknown = error.error;
    const isApiError = typeof body === 'object' && body !== null
      && 'codigo' in body && typeof body.codigo === 'string'
      && 'mensaje' in body && typeof body.mensaje === 'string';
    const detalle: ApiError = isApiError ? body as ApiError : {
      codigo: error.status === 0 ? 'SIN_CONEXION' : 'SERVICIO_NO_DISPONIBLE',
      mensaje: 'No pudimos cargar la información. Comprueba tu conexión e inténtalo de nuevo.',
      errores_campo: {}
    };
    return throwError(() => new ApiClientError(error.status, detalle));
  })
);
