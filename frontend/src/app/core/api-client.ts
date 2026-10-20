import { inject, Injectable, InjectionToken } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Ruta relativa: proxy de desarrollo o reverse proxy del despliegue.
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', { providedIn: 'root', factory: () => '/api/v1' });

@Injectable({ providedIn: 'root' })
export class ApiClient {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  get<T>(path: string): Observable<T> { return this.http.get<T>(`${this.baseUrl}${path}`); }
}
