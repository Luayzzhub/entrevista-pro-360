import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClient } from '../../core/api-client';

export interface Puesto {
  id_puesto: number;
  nombre_puesto: string;
  area: string;
  descripcion: string | null;
  nivel_experiencia: string;
  activo: boolean;
}

export interface Pregunta {
  id_pregunta: number;
  id_puesto: number;
  enunciado: string;
  competencia_evaluada: string;
  orden: number;
  tiempo_limite_seg: number;
  activo: boolean;
  aplica_star: boolean;
}

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly api = inject(ApiClient);
  listarPuestos(): Observable<Puesto[]> { return this.api.get<Puesto[]>('/puestos'); }
  listarPreguntas(id: number): Observable<Pregunta[]> { return this.api.get<Pregunta[]>(`/puestos/${id}/preguntas`); }
}
