import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { API_BASE_URL } from './api-client';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const base = inject(API_BASE_URL);
  const ruta = request.url.split('?')[0];
  const esApi = ruta.startsWith(base + '/');
  const publica = ruta === base + '/puestos' || ruta.startsWith(base + '/puestos/') || ruta.startsWith(base + '/auth/');
  const privada = esApi && !publica;
  const token = privada && auth.sesionVigente() ? auth.accessToken() : null;
  const autenticada = token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  return next(autenticada).pipe(catchError((error: unknown) => {
    if (privada && error instanceof HttpErrorResponse && error.status === 401 && auth.accessToken() === token) auth.cerrarSesion();
    return throwError(() => error);
  }));
};
