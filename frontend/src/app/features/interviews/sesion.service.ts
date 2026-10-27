import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClient } from '../../core/api-client';
import { Pregunta } from './catalogo.service';

export type EstadoSesion = 'INICIADA' | 'EN_PROCESO' | 'COMPLETADA' | 'CANCELADA';
export type EstadoProcesamiento = 'SUBIDO' | 'AUDIO_EXTRAIDO' | 'TRANSCRITO' | 'EVALUADO' | 'FALLIDO';
export interface SesionResumen {
  id_sesion: number; id_puesto: number; nombre_puesto: string; area_puesto: string; nivel_experiencia: string;
  estado_sesion: EstadoSesion; puntaje_promedio: number | null; fecha_inicio: string; fecha_fin: string | null;
  total_preguntas: number; respuestas_confirmadas: number; respuestas_evaluadas: number; orden_pregunta_habilitada: number | null;
}
export interface ResumenSesiones {
  sesion_pendiente: SesionResumen | null; ultima_sesion_completada: SesionResumen | null;
  total_sesiones_completadas: number; items: SesionResumen[];
}
export interface PreguntaSesion {
  id_pregunta: number; orden: number; enunciado: string; competencia_evaluada: string; tiempo_limite_seg: number;
  aplica_star: boolean; habilitada: boolean; id_respuesta: number | null; estado_procesamiento: EstadoProcesamiento | null;
  puntaje_general: number | null; version_rubrica: string | null; puede_reintentar: boolean; motivo_fallo: string | null;
}
export interface SesionDetalle {
  id_sesion: number; id_usuario: number; id_puesto: number; nombre_puesto: string; area_puesto: string; nivel_experiencia: string;
  estado_sesion: EstadoSesion; puntaje_promedio: number | null; fecha_inicio: string; fecha_fin: string | null;
  total_preguntas: number; respuestas_confirmadas: number; respuestas_evaluadas: number;
  pregunta_habilitada: Pregunta | null; preguntas: PreguntaSesion[];
  motivo_cierre: string | null;
}
export function estadoSesionTexto(estado: EstadoSesion): string {
  return { INICIADA: 'Sesión iniciada', EN_PROCESO: 'En proceso', COMPLETADA: 'Completada', CANCELADA: 'Cancelada' }[estado];
}

@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly api = inject(ApiClient);
  consultarResumen(): Observable<ResumenSesiones> { return this.api.get<ResumenSesiones>('/sesiones'); }
  crearSesion(idPuesto: number): Observable<SesionDetalle> { return this.api.post<SesionDetalle>('/sesiones', { id_puesto: idPuesto }); }
  consultarSesion(idSesion: number): Observable<SesionDetalle> { return this.api.get<SesionDetalle>(`/sesiones/${idSesion}`); }
  cancelarSesion(idSesion: number): Observable<SesionDetalle> { return this.api.post<SesionDetalle>(`/sesiones/${idSesion}/cancelacion`, {}); }
}
