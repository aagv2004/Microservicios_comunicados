import { Component, computed, inject, signal, effect, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ForumService } from '../../core/services/forum.service';
import { FormField } from '../../shared/forms/form-field';
import {
  emailValidators,
  nameValidators,
  passwordRules,
  matchingPasswords,
} from '../../shared/forms/validators';

@Component({
  selector: 'app-account-page',
  imports: [ReactiveFormsModule, RouterLink, FormField],
  templateUrl: './account-page.html',
  styleUrl: './account-page.css',
})
export class AccountPage {
  protected readonly auth = inject(AuthService);
  protected readonly forum = inject(ForumService);
  protected readonly myTopics = computed(() => this.forum.myTopics());
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly data = toSignal(this.route.data, { initialValue: this.route.snapshot.data });
  protected readonly mode = computed(() => this.data()['mode'] as string);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly recoveryToken = signal<string | null>(null);
  protected readonly title = computed(
    () =>
      ({
        login: 'Qué bueno verte de nuevo.',
        register: 'Tu próxima conversación empieza aquí.',
        recovery: 'Recupera tu acceso.',
        reset: 'Elige una nueva contraseña.',
        profile: 'Tu espacio en la comunidad.',
      })[this.mode()] ?? 'Tu cuenta',
  );

  protected readonly login = this.fb.group({
    correo: ['', emailValidators],
    password: ['', [Validators.required, Validators.maxLength(64)]],
  });
  protected readonly register = this.fb.group(
    {
      nombre: ['', nameValidators],
      correo: ['', emailValidators],
      password: ['', [Validators.required, passwordRules]],
      confirmacion: ['', Validators.required],
    },
    { validators: matchingPasswords },
  );
  protected readonly recovery = this.fb.group({ correo: ['', emailValidators] });
  protected readonly reset = this.fb.group(
    {
      password: ['', [Validators.required, passwordRules]],
      confirmacion: ['', Validators.required],
    },
    { validators: matchingPasswords },
  );
  protected readonly profile = this.fb.group({
    nombre: ['', nameValidators],
    correo: ['', emailValidators],
    biografia: ['', Validators.maxLength(280)],
  });

  constructor() {
    effect(() => {
      const mode = this.mode();
      this.error.set('');
      this.message.set('');
      this.recoveryToken.set(null);
      this.login.reset();
      this.register.reset();
      this.recovery.reset();
      this.reset.reset();
      if (mode === 'profile') {
        const user = untracked(() => this.auth.user());
        if (user)
          this.profile.reset({
            nombre: user.nombre,
            correo: user.correo,
            biografia: user.biografia,
          });
      }
    });
  }

  protected async submit(): Promise<void> {
    if (this.busy()) return;
    const mode = this.mode();
    const form = {
      login: this.login,
      register: this.register,
      recovery: this.recovery,
      reset: this.reset,
      profile: this.profile,
    }[mode];
    if (!form) return;
    this.error.set('');
    this.message.set('');
    form.markAllAsTouched();
    if (form.invalid) {
      this.error.set('Revisa los campos marcados antes de continuar.');
      return;
    }
    this.busy.set(true);
    try {
      if (mode === 'login') {
        const value = this.login.getRawValue();
        await this.auth.login(value.correo, value.password);
        this.login.reset();
        await this.router.navigateByUrl('/');
      } else if (mode === 'register') {
        const value = this.register.getRawValue();
        await this.auth.register(value.nombre, value.correo, value.password);
        this.register.reset();
        await this.router.navigateByUrl('/perfil');
      } else if (mode === 'recovery') {
        this.recoveryToken.set(await this.auth.requestRecovery(this.recovery.getRawValue().correo));
        this.message.set(
          'Solicitud procesada. Si el correo corresponde a una cuenta de esta demo, se habilitará un enlace de prueba aquí. No se envían correos.',
        );
      } else if (mode === 'reset') {
        const token = this.route.snapshot.queryParamMap.get('token') ?? '';
        await this.auth.resetPassword(token, this.reset.getRawValue().password);
        this.reset.reset();
        this.message.set('Contraseña actualizada en esta demo. Ya puedes iniciar sesión.');
      } else {
        const value = this.profile.getRawValue();
        this.auth.updateProfile(value.nombre, value.correo, value.biografia);
        this.message.set('Los cambios de tu perfil se guardaron en esta sesión.');
      }
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo completar la operación.');
    } finally {
      this.busy.set(false);
    }
  }
}
