import '@angular/compiler';
import test from 'node:test';
import assert from 'node:assert/strict';
import { FormControl, FormGroup } from '@angular/forms';
import { AuthService } from '../target/tests/auth.service.mjs';
import {
  passwordRules,
  matchingPasswords,
  nameValidators,
  emailValidators,
} from '../target/tests/validators.mjs';

const validPassword = 'UnaClave2026!';

test('las cuentas demo conservan sus roles y permiten cerrar sesión', async () => {
  const service = new AuthService();
  await service.login(' ADMIN@NEXO.DEMO ', 'NexoDemo2026!');
  assert.equal(service.user().rol, 'ADMIN');
  service.logout();
  assert.equal(service.user(), null);
  await service.login('usuario@nexo.demo', 'NexoDemo2026!');
  assert.equal(service.user().rol, 'USUARIO');
});

test('el ingreso rechaza contraseña incorrecta y cuenta inexistente', async () => {
  const service = new AuthService();
  await assert.rejects(
    service.login('usuario@nexo.demo', 'Incorrecta2026!'),
    /Correo o contraseña incorrectos/,
  );
  await assert.rejects(
    service.login('ausente@nexo.demo', validPassword),
    /Correo o contraseña incorrectos/,
  );
  assert.equal(service.user(), null);
});

test('registro normaliza el correo y no permite cuentas duplicadas', async () => {
  const service = new AuthService();
  await service.register(' Ana Pérez ', ' ANA@CORREO.CL ', validPassword);
  assert.equal(service.user().nombre, 'Ana Pérez');
  assert.equal(service.user().correo, 'ana@correo.cl');
  assert.equal(service.user().rol, 'USUARIO');
  await assert.rejects(
    service.register('Otra Persona', 'ana@correo.cl', validPassword),
    /Ya existe/,
  );
  const results = await Promise.allSettled([
    service.register('Persona Uno', 'simultaneo@correo.cl', validPassword),
    service.register('Persona Dos', 'simultaneo@correo.cl', validPassword),
  ]);
  assert.equal(results.filter((result) => result.status === 'fulfilled').length, 1);
});

test('perfil exige sesión, rechaza correo duplicado y conserva el rol', async () => {
  const service = new AuthService();
  assert.throws(() => service.updateProfile('Ana', 'ana@correo.cl', ''), /sesión activa/);
  await service.login('usuario@nexo.demo', 'NexoDemo2026!');
  assert.throws(() => service.updateProfile('Ana', 'admin@nexo.demo', ''), /ya está registrado/);
  service.updateProfile(' Ana Pérez ', ' ANA@CORREO.CL ', ' Mi biografía ');
  assert.equal(service.user().rol, 'USUARIO');
  assert.equal(service.user().biografia, 'Mi biografía');
  service.logout();
  await service.login('ana@correo.cl', 'NexoDemo2026!');
  assert.equal(service.user().nombre, 'Ana Pérez');
});

test('recuperación cambia contraseña y el enlace no puede reutilizarse', async () => {
  const service = new AuthService();
  assert.equal(await service.requestRecovery('ausente@correo.cl'), null);
  const token = await service.requestRecovery('usuario@nexo.demo');
  await service.resetPassword(token, validPassword);
  await assert.rejects(service.resetPassword(token, 'OtraClave2026!'), /no es válido/);
  await assert.rejects(service.login('usuario@nexo.demo', 'NexoDemo2026!'));
  await service.login('usuario@nexo.demo', validPassword);
  assert.equal(service.user().rol, 'USUARIO');
});

test('solicitar otro enlace invalida el anterior y se respeta su vencimiento', async () => {
  const service = new AuthService();
  const oldToken = await service.requestRecovery('usuario@nexo.demo');
  const token = await service.requestRecovery('usuario@nexo.demo');
  await assert.rejects(service.resetPassword(oldToken, validPassword));
  const now = Date.now;
  try {
    const expired = now() + 16 * 60 * 1000;
    Date.now = () => expired;
    await assert.rejects(service.resetPassword(token, validPassword), /venció/);
  } finally {
    Date.now = now;
  }
});

test('cada regla de contraseña rechaza su caso inválido', () => {
  assert.equal(passwordRules(new FormControl(validPassword)), null);
  for (const [value, key] of [
    ['Aa1!', 'passwordLength'],
    ['A'.repeat(65) + 'a1!', 'passwordLength'],
    ['UNACLAVE2026!', 'lowercase'],
    ['unaclave2026!', 'uppercase'],
    ['UnaClaveLarga!', 'digit'],
    ['UnaClave2026', 'special'],
    ['Una Clave2026!', 'whitespace'],
  ])
    assert.equal(passwordRules(new FormControl(value))[key], true);
});

test('formularios validan nombre, correo y coincidencia de contraseñas', () => {
  for (const name of ['', '  ', 'A  ', 'Nombre123'])
    assert.equal(new FormControl(name, nameValidators).invalid, true);
  assert.equal(new FormControl('María José', nameValidators).valid, true);
  for (const email of ['', 'correo', 'sin@', 'a'.repeat(151)])
    assert.equal(new FormControl(email, emailValidators).invalid, true);
  assert.equal(new FormControl('nombre@correo.cl', emailValidators).valid, true);
  const group = new FormGroup(
    { password: new FormControl(validPassword), confirmacion: new FormControl('otra') },
    matchingPasswords,
  );
  assert.equal(group.hasError('mismatch'), true);
  group.controls.confirmacion.setValue(validPassword);
  assert.equal(group.valid, true);
});
