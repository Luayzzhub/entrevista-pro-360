import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-panel', imports: [RouterLink],
  template: `
    <section class="dashboard" aria-labelledby="titulo-panel"><div class="dashboard-heading"><span class="eyebrow">TU ESPACIO DE PREPARACIÓN</span><h1 id="titulo-panel">Hola, {{ auth.usuario()?.nombre_completo }}.</h1><p>Da el siguiente paso a tu ritmo. Empieza por conocer las preguntas de tu próximo puesto.</p></div>
      <div class="dashboard-grid"><article class="form-card dashboard-practice"><span class="pill">Tus prácticas</span><h2>Prepárate para tu próxima entrevista</h2><p>La grabación de entrevistas estará disponible en una próxima etapa. Por ahora, explora los puestos y sus tres preguntas.</p><a class="button" routerLink="/puestos">Nueva práctica <span aria-hidden="true">↗</span></a></article>
      <aside class="form-card"><span class="eyebrow">MI CUENTA</span><h2>{{ auth.usuario()?.nombre_completo }}</h2><p class="account-email">{{ auth.usuario()?.email }}</p><p class="account-status">Cuenta activa · Postulante</p><a class="button secondary" routerLink="/perfil">Mi perfil</a></aside></div>
    </section>
  `
})
export class PanelComponent { readonly auth = inject(AuthService); }
