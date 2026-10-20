import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { nombreValidator, emailValidator, passwordValidator, confirmacionValidator } from '../../shared/usuario.validators';
import { erroresDeCampo, mensajeApi } from '../../shared/form-api-error';

@Component({
  selector: 'app-registro', imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="account-layout" aria-labelledby="titulo-registro">
      <aside class="account-intro"><span class="eyebrow">EMPIEZA CON UN PASO PEQUEÑO</span><h1 id="titulo-registro">Tu próxima entrevista<br>empieza contigo.</h1><p>Crea tu cuenta y prepara las ideas que quieres compartir.</p><div class="intro-note">No necesitas experiencia laboral para empezar: también puedes hablar de tus proyectos y aprendizajes.</div></aside>
      <div class="form-card"><h2>Crear cuenta</h2><p class="form-intro">Completa tus datos de identificación.</p>
        <form [formGroup]="form" (ngSubmit)="enviar()" [attr.aria-busy]="enviando()">
          <div class="field"><label for="registro-nombre">Nombre completo *</label><input id="registro-nombre" type="text" autocomplete="name" formControlName="nombre_completo" aria-describedby="registro-nombre-error" [attr.aria-invalid]="invalido('nombre_completo')">
            @if (invalido('nombre_completo')) { <p class="field-error" id="registro-nombre-error">{{ form.controls.nombre_completo.errors?.['api'] || 'Ingresa entre 1 y 150 caracteres, sin contar espacios exteriores.' }}</p> }
          </div>
          <div class="field"><label for="registro-email">Correo electrónico *</label><input id="registro-email" type="email" autocomplete="username" formControlName="email" aria-describedby="registro-email-error" [attr.aria-invalid]="invalido('email')">
            @if (invalido('email')) { <p class="field-error" id="registro-email-error">{{ form.controls.email.errors?.['api'] || 'Ingresa un correo válido de hasta 150 caracteres.' }}</p> }
          </div>
          <div class="field"><label for="registro-password">Contraseña *</label><input id="registro-password" type="password" autocomplete="new-password" formControlName="password" aria-describedby="password-ayuda registro-password-error" [attr.aria-invalid]="invalido('password')"><p class="form-help" id="password-ayuda">Mínimo 12 caracteres Unicode y máximo 72 bytes UTF-8. Los espacios forman parte de tu contraseña.</p>
            @if (invalido('password')) { <p class="field-error" id="registro-password-error">{{ form.controls.password.errors?.['api'] || 'Revisa la longitud: los emojis y las tildes pueden ocupar varios bytes.' }}</p> }
          </div>
          <div class="field"><label for="registro-confirmacion">Confirmación de contraseña *</label><input id="registro-confirmacion" type="password" autocomplete="new-password" formControlName="confirmacion" aria-describedby="confirmacion-error" [attr.aria-invalid]="form.controls.confirmacion.touched && form.hasError('confirmacion')">
            @if (form.controls.confirmacion.touched && form.hasError('confirmacion')) { <p class="field-error" id="confirmacion-error">Las contraseñas deben coincidir exactamente, incluidos los espacios.</p> }
          </div>
          <aside class="notice privacy"><strong>Privacidad y revisión de datos</strong><p>Tu nombre y correo identifican tu cuenta. Revisa que sean correctos antes de continuar. No compartas tu contraseña ni incluyas datos sensibles en tu perfil.</p></aside>
          @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
          <div class="form-actions"><button class="button" type="submit" [disabled]="enviando()">{{ enviando() ? 'Creando cuenta…' : 'Crear cuenta' }}</button><a class="button secondary" routerLink="/login">Volver</a></div>
        </form>
      </div>
    </section>
  `
})
export class RegistroComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = new FormGroup({
    nombre_completo: new FormControl('', { nonNullable: true, validators: [nombreValidator] }),
    email: new FormControl('', { nonNullable: true, validators: [emailValidator] }),
    password: new FormControl('', { nonNullable: true, validators: [passwordValidator] }),
    confirmacion: new FormControl('', { nonNullable: true })
  }, { validators: [confirmacionValidator] });
  invalido(campo: keyof typeof this.form.controls): boolean { const control = this.form.controls[campo]; return control.touched && control.invalid; }
  enviar(): void {
    if (this.enviando()) return;
    this.form.markAllAsTouched(); this.error.set(null);
    if (this.form.invalid) return;
    this.enviando.set(true);
    const { nombre_completo, email, password } = this.form.getRawValue();
    this.auth.registrar({ nombre_completo, email, password }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.enviando.set(false); this.form.reset(); void this.router.navigate(['/login'], { state: { registroExitoso: true } }); },
      error: error => { this.enviando.set(false); this.error.set(mensajeApi(error)); erroresDeCampo(error, this.form); }
    });
  }
}
