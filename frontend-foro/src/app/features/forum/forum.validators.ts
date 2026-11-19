import { AbstractControl, ValidationErrors } from '@angular/forms';
export const trimmedLength =
  (min: number, max: number) =>
  (control: AbstractControl): ValidationErrors | null => {
    const length = String(control.value ?? '').trim().length;
    return length >= min && length <= max ? null : { trimmedLength: true };
  };
