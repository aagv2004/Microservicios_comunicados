import { TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { webcrypto } from 'node:crypto';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import {
  emailValidators,
  matchingPasswords,
  nameValidators,
  passwordRules,
} from '../../shared/forms/validators';

let service: AuthService;
const password = 'UnaClave2026!';
beforeEach(() => {
  vi.stubGlobal('crypto', webcrypto);
  TestBed.configureTestingModule({});
  service = TestBed.inject(AuthService);
});
afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

it('inicia las cuentas de ejemplo con sus roles y cierra sesión', async () => {
  await service.login(' ADMIN@NEXO.DEMO ', 'NexoDemo2026!');
  expect(service.user()?.rol).toBe('ADMIN');
  service.logout();
  expect(service.user()).toBeNull();
  await service.login('usuario@nexo.demo', 'NexoDemo2026!');
  expect(service.user()?.rol).toBe('USUARIO');
});
it('rechaza contraseñas incorrectas y cuentas inexistentes', async () => {
  await expect(service.login('usuario@nexo.demo', 'Incorrecta2026!')).rejects.toThrow(
    'Correo o contraseña incorrectos',
  );
  await expect(service.login('ausente@nexo.demo', password)).rejects.toThrow(
    'Correo o contraseña incorrectos',
  );
  expect(service.user()).toBeNull();
});
it('registra USUARIO, normaliza el correo y evita duplicados incluso simultáneos', async () => {
  await service.register(' Ana Pérez ', ' ANA@CORREO.CL ', password);
  expect(service.user()).toMatchObject({
    nombre: 'Ana Pérez',
    correo: 'ana@correo.cl',
    rol: 'USUARIO',
  });
  await expect(service.register('Otra Persona', 'ana@correo.cl', password)).rejects.toThrow(
    'Ya existe',
  );
  const results = await Promise.allSettled([
    service.register('Persona Uno', 'simultaneo@correo.cl', password),
    service.register('Persona Dos', 'simultaneo@correo.cl', password),
  ]);
  expect(results.filter((result) => result.status === 'fulfilled')).toHaveLength(1);
});
it('perfil requiere sesión, conserva el rol y rechaza correos duplicados', async () => {
  expect(() => service.updateProfile('Ana', 'ana@correo.cl', '')).toThrow('sesión activa');
  await service.login('usuario@nexo.demo', 'NexoDemo2026!');
  expect(() => service.updateProfile('Ana', 'admin@nexo.demo', '')).toThrow('ya está registrado');
  service.updateProfile(' Ana Pérez ', ' ANA@CORREO.CL ', ' Mi biografía ');
  expect(service.user()).toMatchObject({
    nombre: 'Ana Pérez',
    biografia: 'Mi biografía',
    rol: 'USUARIO',
  });
  service.logout();
  await service.login('ana@correo.cl', 'NexoDemo2026!');
  expect(service.user()?.nombre).toBe('Ana Pérez');
});
it('recuperación cambia la contraseña y permite usar el enlace una sola vez', async () => {
  expect(await service.requestRecovery('ausente@correo.cl')).toBeNull();
  const token = (await service.requestRecovery('usuario@nexo.demo'))!;
  await service.resetPassword(token, password);
  await expect(service.resetPassword(token, 'OtraClave2026!')).rejects.toThrow('no es válido');
  await expect(service.login('usuario@nexo.demo', 'NexoDemo2026!')).rejects.toThrow();
  await service.login('usuario@nexo.demo', password);
  expect(service.user()?.rol).toBe('USUARIO');
});
it('una nueva recuperación invalida la anterior y respeta el vencimiento', async () => {
  const oldToken = (await service.requestRecovery('usuario@nexo.demo'))!;
  const token = (await service.requestRecovery('usuario@nexo.demo'))!;
  await expect(service.resetPassword(oldToken, password)).rejects.toThrow();
  vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 16 * 60 * 1000);
  await expect(service.resetPassword(token, password)).rejects.toThrow('venció');
});
it('valida cada regla de contraseña', () => {
  expect(passwordRules(new FormControl(password))).toBeNull();
  const cases: [string, string][] = [
    ['Aa1!', 'passwordLength'],
    ['A'.repeat(65) + 'a1!', 'passwordLength'],
    ['UNACLAVE2026!', 'lowercase'],
    ['unaclave2026!', 'uppercase'],
    ['UnaClaveLarga!', 'digit'],
    ['UnaClave2026', 'special'],
    ['Una Clave2026!', 'whitespace'],
  ];
  for (const [value, key] of cases) expect(passwordRules(new FormControl(value))?.[key]).toBe(true);
});
it('valida nombre, correo y confirmación de contraseña', () => {
  for (const name of ['', '  ', 'A  ', 'Nombre123'])
    expect(new FormControl(name, nameValidators).invalid).toBe(true);
  expect(new FormControl('María José', nameValidators).valid).toBe(true);
  for (const email of ['', 'correo', 'sin@', 'a'.repeat(151)])
    expect(new FormControl(email, emailValidators).invalid).toBe(true);
  expect(new FormControl('nombre@correo.cl', emailValidators).valid).toBe(true);
  const group = new FormGroup(
    { password: new FormControl(password), confirmacion: new FormControl('otra') },
    matchingPasswords,
  );
  expect(group.hasError('mismatch')).toBe(true);
  group.controls.confirmacion.setValue(password);
  expect(group.valid).toBe(true);
});
