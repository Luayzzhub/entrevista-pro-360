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
  version_rubrica: string | null; motivo_cierre: string | null;
}
export interface PuntoEvolucion {
  id_sesion: number; fecha_inicio: string; fecha_fin: string; puntaje_promedio: number; version_rubrica: string;
}
export interface ComparacionHistorial {
  habilitada: boolean; id_puesto: number | null; nombre_puesto: string | null; version_rubrica: string | null;
  total_sesiones_comparadas: number; promedio_conjunto: number | null; mejor_puntaje: number | null;
  cambio_vs_primera: number | null; cambio_vs_anterior: number | null; serie: PuntoEvolucion[];
}
export interface FiltrosHistorial {
  id_puesto?: number; fecha_desde?: string; fecha_hasta?: string; estado?: EstadoSesion; version_rubrica?: string;
  page?: number; size?: number;
}
export interface ResumenSesiones {
  sesion_pendiente: SesionResumen | null; ultima_sesion_completada: SesionResumen | null;
  total_sesiones_completadas: number; items: SesionResumen[];
  pagina: number; tamano_pagina: number; total_items_filtrados: number; total_paginas: number;
  aviso_filtro_rubrica: string | null; comparacion: ComparacionHistorial;
  puestos_historial: { id_puesto: number; nombre_puesto: string }[];
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
  consultarResumen(filtros: FiltrosHistorial = {}): Observable<ResumenSesiones> {
    const params = new URLSearchParams();
    for (const [clave, valor] of Object.entries(filtros)) if (valor !== undefined && valor !== '') params.set(clave, String(valor));
    return this.api.get<ResumenSesiones>('/sesiones' + (params.size ? '?' + params.toString() : ''));
  }
  crearSesion(idPuesto: number): Observable<SesionDetalle> { return this.api.post<SesionDetalle>('/sesiones', { id_puesto: idPuesto }); }
  consultarSesion(idSesion: number): Observable<SesionDetalle> { return this.api.get<SesionDetalle>(`/sesiones/${idSesion}`); }
  cancelarSesion(idSesion: number): Observable<SesionDetalle> { return this.api.post<SesionDetalle>(`/sesiones/${idSesion}/cancelacion`, {}); }
}
