import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from './api-client';
import { LoginResponse, PerfilRequest, RegistroRequest, UsuarioPublico } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = inject(API_BASE_URL);
  private readonly tokenState = signal<string | null>(null);
  private readonly usuarioState = signal<UsuarioPublico | null>(null);
  private readonly vencimiento = signal(0);
  private temporizador: ReturnType<typeof setTimeout> | null = null;
  readonly accessToken = this.tokenState.asReadonly();
  readonly usuario = this.usuarioState.asReadonly();
  readonly autenticado = computed(() => this.tokenState() !== null && this.usuarioState() !== null);

  registrar(datos: RegistroRequest): Observable<UsuarioPublico> { return this.http.post<UsuarioPublico>(`${this.baseUrl}/auth/registro`, datos); }
  iniciarSesion(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`, { email, password }).pipe(tap(respuesta => {
      this.limpiarEstado();
      this.tokenState.set(respuesta.access_token);
      this.usuarioState.set(respuesta.usuario);
      this.vencimiento.set(Date.now() + respuesta.expires_in * 1000);
      this.temporizador = setTimeout(() => this.cerrarSesion(), respuesta.expires_in * 1000);
    }));
  }
  sesionVigente(): boolean {
    if (this.autenticado() && Date.now() >= this.vencimiento()) this.cerrarSesion();
    return this.autenticado();
  }
  consultarPerfil(): Observable<UsuarioPublico> {
    const token = this.accessToken();
    return this.http.get<UsuarioPublico>(`${this.baseUrl}/perfil`).pipe(tap(usuario => this.actualizarUsuarioVigente(token, usuario)));
  }
  actualizarPerfil(datos: PerfilRequest): Observable<UsuarioPublico> {
    const token = this.accessToken();
    return this.http.patch<UsuarioPublico>(`${this.baseUrl}/perfil`, datos).pipe(tap(usuario => this.actualizarUsuarioVigente(token, usuario)));
  }
  cerrarSesion(): void {
    this.limpiarEstado();
    void this.router.navigate(['/login']);
  }
  private actualizarUsuarioVigente(token: string | null, usuario: UsuarioPublico): void {
    if (token !== null && this.accessToken() === token && this.sesionVigente()) this.usuarioState.set(usuario);
  }
  private limpiarEstado(): void {
    if (this.temporizador !== null) clearTimeout(this.temporizador);
    this.temporizador = null;
    this.tokenState.set(null);
    this.usuarioState.set(null);
    this.vencimiento.set(0);
  }
}
