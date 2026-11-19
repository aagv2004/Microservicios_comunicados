import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

export const nameValidators = [
  Validators.required,
  (control: AbstractControl): ValidationErrors | null =>
    String(control.value ?? '').trim().length >= 3 ? null : { minlength: true },
  Validators.maxLength(100),
  Validators.pattern(/^[\p{L}\p{M}][\p{L}\p{M} .'-]*$/u),
];
export const emailValidators = [Validators.required, Validators.email, Validators.maxLength(150)];
export const passwordRules: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = String(control.value ?? '');
  const errors: ValidationErrors = {};
  if (value.length < 10 || value.length > 64) errors['passwordLength'] = true;
  if (!/[a-z]/.test(value)) errors['lowercase'] = true;
  if (!/[A-Z]/.test(value)) errors['uppercase'] = true;
  if (!/[0-9]/.test(value)) errors['digit'] = true;
  if (!/[^A-Za-z0-9\s]/.test(value)) errors['special'] = true;
  if (/\s/.test(value)) errors['whitespace'] = true;
  return Object.keys(errors).length ? errors : null;
};
export const matchingPasswords: ValidatorFn = (group: AbstractControl) =>
  group.get('password')?.value === group.get('confirmacion')?.value ? null : { mismatch: true };
