import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const nombreValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const nombre = typeof control.value === 'string' ? control.value.trim() : '';
  return nombre.length >= 1 && nombre.length <= 150 ? null : { nombre: true };
};
export const emailValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const email = typeof control.value === 'string' ? control.value.trim() : '';
  return email.length <= 150 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? null : { email: true };
};
export const passwordValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = typeof control.value === 'string' ? control.value : '';
  return Array.from(password).length >= 12 && new TextEncoder().encode(password).length <= 72 ? null : { password: true };
};
export const confirmacionValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  control.get('password')?.value === control.get('confirmacion')?.value ? null : { confirmacion: true };
export const resumenValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value === 'string' && control.value.trim().length > 500 ? { resumen: true } : null;
