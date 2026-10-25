import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../core/api-client';
import { EstadoProcesamiento, EstadoSesion } from './sesion.service';
export interface ConfirmacionRespuesta {
  id_respuesta: number; id_sesion: number; id_pregunta: number; orden_pregunta: number;
  estado_sesion: EstadoSesion; estado_procesamiento: EstadoProcesamiento; etapa_actual: string | null; ultimo_hito_valido: string | null;
  duracion_segundos: number; formato: 'webm' | 'mp4'; tamano_bytes: number; fecha_grabacion: string; ruta_consulta: string;
}
export interface RespuestaEstado extends Omit<ConfirmacionRespuesta, 'ruta_consulta'> {
  enunciado_pregunta: string; competencia_evaluada: string; aplica_star: boolean; nombre_puesto: string;
  puede_reintentar: boolean; error: string | null;
}
@Injectable({ providedIn: 'root' })
export class RespuestaService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  confirmarRespuesta(sesion: number, pregunta: number, blob: Blob, contenedor: 'webm' | 'mp4'): Observable<HttpEvent<ConfirmacionRespuesta>> {
    const data = new FormData(); data.append('video', blob, 'respuesta.' + contenedor);
    return this.http.put<ConfirmacionRespuesta>(`${this.base}/sesiones/${sesion}/preguntas/${pregunta}/respuesta`, data, { reportProgress: true, observe: 'events' });
  }
  consultarRespuesta(id: number): Observable<RespuestaEstado> { return this.http.get<RespuestaEstado>(`${this.base}/respuestas/${id}`); }
}
