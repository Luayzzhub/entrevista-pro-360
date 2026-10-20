import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { emailValidator, passwordValidator } from '../../shared/usuario.validators';
import { erroresDeCampo, mensajeApi } from '../../shared/form-api-error';

@Component({
  selector: 'app-login', imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="account-layout" aria-labelledby="titulo-login">
      <aside class="account-intro"><span class="eyebrow">VUELVE A TU PRÓXIMO PASO</span><h1 id="titulo-login">Practica con intención.<br>Habla con confianza.</h1><p>Entra a tu cuenta para preparar tu entrevista y mantener tu perfil al día.</p><div class="intro-note">Tu experiencia de clase, tus proyectos y lo que has aprendido también cuentan.</div></aside>
      <div class="form-card"><h2>Iniciar sesión</h2><p class="form-intro">Qué bueno tenerte de vuelta.</p>
        @if (registroExitoso) { <p class="notice success" role="status">Tu cuenta se creó correctamente. Inicia sesión para continuar.</p> }
        <form [formGroup]="form" (ngSubmit)="enviar()" [attr.aria-busy]="enviando()">
          <div class="field"><label for="login-email">Correo electrónico</label><input id="login-email" type="email" autocomplete="username" formControlName="email" aria-describedby="login-email-error" [attr.aria-invalid]="form.controls.email.touched && form.controls.email.invalid">
            @if (form.controls.email.touched && form.controls.email.invalid) { <p class="field-error" id="login-email-error">{{ form.controls.email.errors?.['api'] || 'Ingresa un correo válido de hasta 150 caracteres.' }}</p> }
          </div>
          <div class="field"><label for="login-password">Contraseña</label><input id="login-password" type="password" autocomplete="current-password" formControlName="password" aria-describedby="login-password-error" [attr.aria-invalid]="form.controls.password.touched && form.controls.password.invalid">
            @if (form.controls.password.touched && form.controls.password.invalid) { <p class="field-error" id="login-password-error">{{ form.controls.password.errors?.['api'] || 'Ingresa tu contraseña: al menos 12 caracteres Unicode y máximo 72 bytes UTF-8.' }}</p> }
          </div>
          @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
          <div class="form-actions"><button class="button" type="submit" [disabled]="enviando()">{{ enviando() ? 'Ingresando…' : 'Iniciar sesión' }}</button><a class="button secondary" routerLink="/registro">Crear cuenta</a></div>
          <p class="form-help">La sesión dura 30 minutos. Al recargar la página deberás iniciar sesión de nuevo.</p>
        </form>
      </div>
    </section>
  `
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly registroExitoso = this.router.currentNavigation()?.extras.state?.['registroExitoso'] === true;
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = new FormGroup({ email: new FormControl('', { nonNullable: true, validators: [emailValidator] }), password: new FormControl('', { nonNullable: true, validators: [passwordValidator] }) });
  enviar(): void {
    if (this.enviando()) return;
    this.form.markAllAsTouched(); this.error.set(null);
    if (this.form.invalid) return;
    this.enviando.set(true);
    const datos = this.form.getRawValue();
    this.auth.iniciarSesion(datos.email, datos.password).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.enviando.set(false); void this.router.navigate(['/panel']); },
      error: error => { this.enviando.set(false); this.error.set(mensajeApi(error)); erroresDeCampo(error, this.form); }
    });
  }
}
