export interface UsuarioPublico {
  id_usuario: number;
  email: string;
  nombre_completo: string;
  rol: 'POSTULANTE';
  estado: 'ACTIVO';
  fecha_registro: string;
  fecha_actualizacion: string | null;
  resumen_perfil: string | null;
}

export interface LoginResponse {
  access_token: string;
  token_type: 'Bearer';
  expires_in: number;
  usuario: UsuarioPublico;
}

export interface RegistroRequest { nombre_completo: string; email: string; password: string; }
export interface PerfilRequest { nombre_completo: string; resumen_perfil: string | null; }
