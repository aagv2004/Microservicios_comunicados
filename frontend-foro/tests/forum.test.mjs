import '@angular/compiler';
import test from 'node:test';
import assert from 'node:assert/strict';
import { Injector, runInInjectionContext } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../target/tests/auth.service.mjs';
import { ForumService } from '../target/tests/forum.service.mjs';
import { adminGuard } from '../target/tests/admin.guard.mjs';

function setup() {
  const auth = new AuthService();
  const injector = Injector.create({
    providers: [{ provide: AuthService, useValue: auth }, ForumService],
  });
  return { auth, forum: injector.get(ForumService) };
}
const input = {
  title: 'Una conversación nueva',
  categoryId: 'tecnologia',
  content: 'Una idea para conversar',
};
const login = (auth, role = 'usuario') => auth.login(role + '@nexo.demo', 'NexoDemo2026!');

test('lectura pública y acciones que requieren sesión', () => {
  const { forum } = setup();
  assert.equal(forum.topics().length, 5);
  assert.equal(
    forum.topics().every((topic) => topic.replies === 0),
    true,
  );
  assert.equal(forum.topic(999), undefined);
  assert.throws(() => forum.publish(input), /Inicia sesión/);
  assert.throws(() => forum.comment(1, 'Comentario'), /Inicia sesión/);
  assert.throws(() => forum.moderationItems(), /Inicia sesión/);
});

test('autor publica, edita y elimina contenido con contadores reales', async () => {
  const { auth, forum } = setup();
  await login(auth);
  const id = forum.publish({ ...input, title: '  Nuevo tema  ' });
  assert.equal(forum.topic(id).title, 'Nuevo tema');
  assert.equal(forum.topic(id).authorId, auth.user().id);
  assert.equal(forum.topics()[0].id, id);
  forum.updateTopic(id, { ...input, title: 'Tema actualizado' });
  const comment = forum.comment(id, '  Respuesta  ');
  assert.equal(forum.comments(id)[0].content, 'Respuesta');
  assert.equal(forum.topics()[0].replies, 1);
  assert.throws(() => forum.deleteTopic(id), /tiene comentarios/);
  forum.updateComment(comment, 'Respuesta editada');
  assert.equal(forum.comments(id)[0].content, 'Respuesta editada');
  forum.deleteComment(comment);
  assert.equal(forum.topics()[0].replies, 0);
  forum.deleteTopic(id);
  assert.equal(forum.topic(id), undefined);
});

test('otro usuario y ADMIN no editan ni eliminan contenido ajeno', async () => {
  const { auth, forum } = setup();
  await login(auth);
  const id = forum.publish(input);
  const comment = forum.comment(id, 'Comentario propio');
  await auth.register('Otra Persona', 'otra@correo.cl', 'OtraClave2026!');
  for (const role of ['otro', 'admin']) {
    if (role === 'admin') await login(auth, 'admin');
    assert.equal(forum.canEdit(forum.topic(id)), false);
    assert.throws(() => forum.updateTopic(id, input), /Solo el autor/);
    assert.throws(() => forum.deleteTopic(id), /Solo el autor/);
    assert.throws(() => forum.updateComment(comment, 'Ajeno'), /Solo el autor/);
    assert.throws(() => forum.deleteComment(comment), /Solo el autor/);
  }
});

test('solo ADMIN modera y los bloqueos individuales sobreviven a restaurar el tema', async () => {
  const { auth, forum } = setup();
  await login(auth);
  const id = forum.publish(input);
  const comment = forum.comment(id, 'Contenido');
  assert.throws(() => forum.moderate('topic', id, true, 'Normas'), /ADMIN/);
  assert.throws(() => forum.moderationItems(), /ADMIN/);
  await login(auth, 'admin');
  forum.moderate('comment', comment, true, 'Spam');
  forum.moderate('topic', id, true, 'Revisión');
  assert.equal(forum.topic(id), undefined);
  assert.equal(
    forum.topics().some((topic) => topic.id === id),
    false,
  );
  assert.deepEqual(forum.comments(id), []);
  assert.equal(forum.moderationItems().find((item) => item.type === 'comment').parentBlocked, true);
  await login(auth);
  assert.throws(() => forum.updateTopic(id, input), /no está disponible/);
  assert.throws(() => forum.comment(id, 'Intento'), /no está disponible/);
  assert.throws(() => forum.updateComment(comment, 'Intento'), /no está disponible/);
  await login(auth, 'admin');
  forum.moderate('topic', id, false, 'Revisión aprobada');
  assert.equal(forum.topic(id).moderatorId, auth.user().id);
  assert.equal(forum.topic(id).moderationReason, 'Revisión aprobada');
  assert.deepEqual(forum.comments(id), []);
  assert.equal(forum.topics().find((topic) => topic.id === id).replies, 0);
  forum.moderate('comment', comment, false, 'Restaurado');
  assert.equal(forum.comments(id).length, 1);
  assert.equal(forum.topics().find((topic) => topic.id === id).replies, 1);
});

test('un autor no puede editar ni borrar un comentario bloqueado', async () => {
  const { auth, forum } = setup();
  await login(auth);
  const id = forum.comment(1, 'Respuesta');
  await login(auth, 'admin');
  forum.moderate('comment', id, true, 'Normas');
  await login(auth);
  assert.throws(() => forum.updateComment(id, 'Intento'), /Solo el autor/);
  assert.throws(() => forum.deleteComment(id), /Solo el autor/);
});

test('validaciones del servicio rechazan contenido vacío, tamaños y categorías inválidas', async () => {
  const { auth, forum } = setup();
  await login(auth);
  for (const data of [
    { ...input, title: '  ' },
    { ...input, title: 'x'.repeat(151) },
    { ...input, content: ' ' },
    { ...input, content: 'x'.repeat(10001) },
    { ...input, categoryId: 'no-existe' },
  ])
    assert.throws(() => forum.publish(data));
  assert.throws(() => forum.comment(1, ' '));
  assert.throws(() => forum.comment(1, 'x'.repeat(5001)));
  assert.throws(() => forum.comment(999, 'Contenido'));
  await login(auth, 'admin');
  assert.throws(() => forum.moderate('topic', 1, true, ' '));
  assert.throws(() => forum.moderate('topic', 1, true, 'x'.repeat(501)));
  assert.throws(() => forum.moderate('topic', 999, true, 'Normas'));
});

test('los resultados son copias y el texto HTML se conserva como contenido', async () => {
  const { auth, forum } = setup();
  await login(auth);
  const text = '<script>alert(1)</script>';
  const id = forum.publish({ ...input, content: text });
  const copy = forum.topic(id);
  copy.content = 'Alterado';
  assert.equal(forum.topic(id).content, text);
  auth.updateProfile('Nuevo Nombre', 'usuario@nexo.demo', '');
  assert.equal(forum.topic(id).author, 'Nuevo Nombre');
});

test('guard de moderación redirige a anónimo y USUARIO y permite ADMIN', async () => {
  const auth = new AuthService();
  const router = { createUrlTree: (commands) => commands[0] };
  const injector = Injector.create({
    providers: [
      { provide: AuthService, useValue: auth },
      { provide: Router, useValue: router },
    ],
  });
  const guard = () => runInInjectionContext(injector, () => adminGuard({}, {}));
  assert.equal(guard(), '/ingresar');
  await login(auth);
  assert.equal(guard(), '/');
  await login(auth, 'admin');
  assert.equal(guard(), true);
});

test('mis publicaciones incluye bloqueadas propias y excluye las de otras cuentas', async () => {
  const { auth, forum } = setup();
  assert.deepEqual(forum.myTopics(), []);
  await login(auth);
  const id = forum.publish(input);
  forum.comment(id, 'Respuesta');
  assert.equal(forum.myTopics().length, 1);
  assert.equal(forum.myTopics()[0].replies, 1);
  await login(auth, 'admin');
  assert.deepEqual(forum.myTopics(), []);
  forum.moderate('topic', id, true, 'Revisión de normas');
  await login(auth);
  assert.equal(forum.myTopics().length, 1);
  assert.equal(forum.myTopics()[0].blocked, true);
  assert.equal(forum.myTopics()[0].moderationReason, 'Revisión de normas');
  assert.equal(forum.topic(id), undefined);
});

test('publicar y abrir el detalle conserva la sesión y el contenido al volver a ingresar', async () => {
  const { auth, forum } = setup();
  await login(auth);
  const userId = auth.user().id;
  const id = forum.publish(input);
  assert.equal(forum.topic(id).title, input.title);
  assert.equal(auth.user().id, userId);
  auth.logout();
  assert.deepEqual(forum.myTopics(), []);
  await login(auth);
  assert.equal(auth.user().id, userId);
  assert.equal(forum.myTopics()[0].id, id);
  assert.equal(forum.topic(id).authorId, userId);
});

test('panel actualiza estado, mensaje y acción al bloquear y restaurar', async () => {
  const { FormBuilder } = await import('@angular/forms');
  const { ModerationPage } = await import('../target/tests/moderation-page.mjs');
  const { auth, forum } = setup();
  await login(auth, 'admin');
  const injector = Injector.create({
    providers: [
      { provide: AuthService, useValue: auth },
      { provide: ForumService, useValue: forum },
      FormBuilder,
    ],
  });
  const page = runInInjectionContext(injector, () => new ModerationPage());
  const item = page.items().find((item) => item.type === 'topic' && item.id === 1);
  page.select(item);
  page.form.controls.reason.setValue('Revisión de normas');
  page.submit();
  assert.equal(page.error(), '');
  assert.equal(page.message(), 'Contenido bloqueado.');
  assert.equal(page.selected(), null);
  assert.equal(page.items().find((item) => item.type === 'topic' && item.id === 1).blocked, true);
  page.filter.set('blocked');
  assert.equal(page.items().length, 1);
  page.select(page.items()[0]);
  page.form.controls.reason.setValue('Revisión aprobada');
  page.submit();
  assert.equal(page.message(), 'Contenido restaurado.');
  assert.equal(page.items().length, 0);
});

test('acceso desde un comentario abre su decisión y bloquea solo ese comentario', async () => {
  const { FormBuilder } = await import('@angular/forms');
  const { ActivatedRoute, convertToParamMap } = await import('@angular/router');
  const { ModerationPage } = await import('../target/tests/moderation-page.mjs');
  const { auth, forum } = setup();
  await login(auth);
  const commentId = forum.comment(1, 'Comentario para moderar');
  const otherId = forum.comment(1, 'Comentario que permanece visible');
  await login(auth, 'admin');
  const route = {
    snapshot: { queryParamMap: convertToParamMap({ tipo: 'comment', id: String(commentId) }) },
  };
  const injector = Injector.create({
    providers: [
      { provide: AuthService, useValue: auth },
      { provide: ForumService, useValue: forum },
      { provide: ActivatedRoute, useValue: route },
      FormBuilder,
    ],
  });
  const page = runInInjectionContext(injector, () => new ModerationPage());
  assert.equal(page.type(), 'comment');
  assert.equal(page.selected().type, 'comment');
  assert.equal(page.selected().id, commentId);
  page.form.controls.reason.setValue('Incumplimiento de normas');
  page.submit();
  assert.equal(page.message(), 'Contenido bloqueado.');
  assert.ok(forum.topic(1));
  assert.deepEqual(
    forum.comments(1).map((comment) => comment.id),
    [otherId],
  );
  assert.equal(page.items().find((item) => item.id === commentId).blocked, true);
});
