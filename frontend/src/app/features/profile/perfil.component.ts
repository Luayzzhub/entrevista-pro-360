import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { nombreValidator, resumenValidator } from '../../shared/usuario.validators';
import { erroresDeCampo, mensajeApi } from '../../shared/form-api-error';

@Component({
  selector: 'app-perfil', imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="profile-page" aria-labelledby="titulo-perfil"><a class="text-link back-link" routerLink="/panel">← Volver al panel</a><span class="eyebrow">TU CUENTA</span><h1 id="titulo-perfil">Mi perfil</h1><p class="intro">Mantén tu nombre y tu presentación al día.</p>
      @if (cargando()) { <p class="status-panel" role="status">Cargando tu perfil…</p> }
      @else if (errorCarga()) { <div class="status-panel error" role="alert"><p>{{ errorCarga() }}</p><button type="button" class="button" (click)="cargar()">Volver a intentar</button></div> }
      @else {
        <div class="form-card"><form [formGroup]="form" (ngSubmit)="guardar()" [attr.aria-busy]="guardando()">
          <div class="field"><label for="perfil-nombre">Nombre completo *</label><input id="perfil-nombre" type="text" autocomplete="name" formControlName="nombre_completo" aria-describedby="perfil-nombre-error" [attr.aria-invalid]="form.controls.nombre_completo.touched && form.controls.nombre_completo.invalid">
            @if (form.controls.nombre_completo.touched && form.controls.nombre_completo.invalid) { <p class="field-error" id="perfil-nombre-error">{{ form.controls.nombre_completo.errors?.['api'] || 'Ingresa entre 1 y 150 caracteres, sin contar espacios exteriores.' }}</p> }
          </div>
          <div class="field"><label for="perfil-email">Correo de identificación (solo lectura)</label><input id="perfil-email" type="email" [value]="auth.usuario()?.email || ''" readonly aria-describedby="perfil-email-ayuda"><p class="form-help" id="perfil-email-ayuda">Este correo identifica tu cuenta y no puede editarse desde el perfil.</p></div>
          <div class="field"><label for="perfil-resumen">Resumen académico o profesional (opcional)</label><textarea id="perfil-resumen" rows="6" maxlength="500" formControlName="resumen_perfil" aria-describedby="resumen-contador perfil-resumen-error" [attr.aria-invalid]="form.controls.resumen_perfil.touched && form.controls.resumen_perfil.invalid"></textarea><p class="form-help character-count" id="resumen-contador">{{ form.controls.resumen_perfil.value.length }} / 500 caracteres · Texto plano</p>
            @if (form.controls.resumen_perfil.touched && form.controls.resumen_perfil.invalid) { <p class="field-error" id="perfil-resumen-error">{{ form.controls.resumen_perfil.errors?.['api'] || 'El resumen admite hasta 500 caracteres.' }}</p> }
          </div>
          @if (exito()) { <p class="notice success" role="status">Tu perfil se guardó correctamente.</p> }
          @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
          <div class="form-actions"><button class="button" type="submit" [disabled]="guardando()">{{ guardando() ? 'Guardando…' : 'Guardar cambios' }}</button><a class="button secondary" routerLink="/panel">Volver</a></div>
        </form></div>
      }
    </section>
  `
})
export class PerfilComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly errorCarga = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly exito = signal(false);
  readonly form = new FormGroup({ nombre_completo: new FormControl('', { nonNullable: true, validators: [nombreValidator] }), resumen_perfil: new FormControl('', { nonNullable: true, validators: [resumenValidator] }) });
  ngOnInit(): void { this.cargar(); }
  cargar(): void {
    this.cargando.set(true); this.errorCarga.set(null);
    this.auth.consultarPerfil().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: usuario => { this.form.reset({ nombre_completo: usuario.nombre_completo, resumen_perfil: usuario.resumen_perfil ?? '' }); this.cargando.set(false); },
      error: error => { this.cargando.set(false); this.errorCarga.set(mensajeApi(error)); }
    });
  }
  guardar(): void {
    if (this.guardando()) return;
    this.form.markAllAsTouched(); this.error.set(null); this.exito.set(false);
    if (this.form.invalid) return;
    this.guardando.set(true);
    const datos = this.form.getRawValue();
    this.auth.actualizarPerfil({ nombre_completo: datos.nombre_completo, resumen_perfil: datos.resumen_perfil }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: usuario => { this.form.reset({ nombre_completo: usuario.nombre_completo, resumen_perfil: usuario.resumen_perfil ?? '' }); this.guardando.set(false); this.exito.set(true); },
      error: error => { this.guardando.set(false); this.error.set(mensajeApi(error)); erroresDeCampo(error, this.form); }
    });
  }
}
