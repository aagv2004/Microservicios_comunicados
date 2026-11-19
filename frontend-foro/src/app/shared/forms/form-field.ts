import { Component, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-form-field',
  imports: [ReactiveFormsModule],
  template: `
    <div class="mb-3">
      <label class="form-label" [for]="id()">{{ label() }}</label>
      <div class="input-group">
        <input
          class="form-control"
          [id]="id()"
          [formControl]="control()"
          [type]="type() === 'password' && visible() ? 'text' : type()"
          [autocomplete]="autocomplete()"
          [attr.name]="id()"
          [required]="true"
          [attr.maxlength]="maxLength()"
          [attr.minlength]="minLength()"
          [attr.aria-invalid]="invalid()"
          [attr.aria-describedby]="id() + '-feedback'"
          [class.is-invalid]="invalid()"
        />
        @if (type() === 'password') {
          <button
            type="button"
            class="btn btn-outline-secondary"
            (click)="visible.set(!visible())"
            [attr.aria-label]="(visible() ? 'Ocultar ' : 'Mostrar ') + label().toLowerCase()"
            [attr.aria-pressed]="visible()"
          >
            {{ visible() ? 'Ocultar' : 'Mostrar' }}
          </button>
        }
      </div>
      <div
        [id]="id() + '-feedback'"
        [class.text-danger]="invalid()"
        class="field-hint mt-1"
        aria-live="polite"
      >
        @if (invalid()) {
          {{ error() }}
        } @else {
          {{ hint() }}
        }
      </div>
    </div>
  `,
})
export class FormField {
  readonly id = input.required<string>();
  readonly label = input.required<string>();
  readonly control = input.required<FormControl<string>>();
  readonly type = input('text');
  readonly autocomplete = input('off');
  readonly maxLength = input<number | null>(null);
  readonly minLength = input<number | null>(null);
  readonly hint = input('');
  readonly mismatch = input(false);
  protected readonly visible = signal(false);
  protected invalid(): boolean {
    return this.control().touched && (this.control().invalid || this.mismatch());
  }
  protected error(): string {
    const control = this.control();
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('email')) return 'Ingresa un correo válido, por ejemplo nombre@correo.cl.';
    if (control.hasError('minlength')) return 'El nombre debe tener al menos 3 caracteres.';
    if (control.hasError('maxlength')) return 'Se superó el largo máximo permitido.';
    if (control.hasError('pattern')) return 'Usa letras y separadores habituales para el nombre.';
    if (this.mismatch()) return 'Las contraseñas no coinciden.';
    return 'Usa entre 10 y 64 caracteres, mayúscula, minúscula, número y símbolo, sin espacios.';
  }
}
