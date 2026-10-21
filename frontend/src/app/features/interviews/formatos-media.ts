export interface FormatoGrabacion { readonly mimeType: string; readonly contenedor: 'webm' | 'mp4'; readonly descripcion: string; }

export const FORMATOS_GRABACION: readonly FormatoGrabacion[] = [
  { mimeType: 'video/webm;codecs=vp8,opus', contenedor: 'webm', descripcion: 'WebM · VP8 + Opus' },
  { mimeType: 'video/webm;codecs=vp9,opus', contenedor: 'webm', descripcion: 'WebM · VP9 + Opus' },
  { mimeType: 'video/mp4;codecs=avc1.42E01E,mp4a.40.2', contenedor: 'mp4', descripcion: 'MP4 · H.264 + AAC' }
];
export const MAXIMO_BYTES_TOMA = 50_000_000;
export const MENSAJE_INCOMPATIBLE = 'Tu navegador no admite los formatos de grabación requeridos (WebM VP8/VP9 con Opus o MP4 H.264 con AAC). Utiliza una versión actualizada de Chrome, Edge o Firefox.';

export function seleccionarFormato(soporta: (mime: string) => boolean): FormatoGrabacion | null {
  return FORMATOS_GRABACION.find(formato => soporta(formato.mimeType)) ?? null;
}

export function validarTomaLocal(bytes: number, duracionSegundos: number): string[] {
  const errores: string[] = [];
  if (!Number.isSafeInteger(bytes) || bytes <= 0) errores.push('El archivo está vacío o su tamaño no es válido.');
  else if (bytes >= MAXIMO_BYTES_TOMA) errores.push('El archivo debe ocupar menos de 50 MB (50 000 000 bytes).');
  if (!Number.isFinite(duracionSegundos) || duracionSegundos < 1) errores.push('La toma debe durar al menos 1 segundo.');
  else if (duracionSegundos > 60) errores.push('La toma no puede superar los 60 segundos.');
  return errores;
}
