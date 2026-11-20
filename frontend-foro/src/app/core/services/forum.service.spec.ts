import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import {
  ActivatedRoute,
  ActivatedRouteSnapshot,
  convertToParamMap,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { webcrypto } from 'node:crypto';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { ForumService } from './forum.service';
import { adminGuard } from '../guards/admin.guard';
import { ModerationPage } from '../../features/forum/moderation-page';
import { ModerationItem } from '../models/forum';

class ModerationPageTest extends ModerationPage {
  get viewItems() {
    return this.items();
  }
  get statusMessage() {
    return this.message();
  }
  get decision() {
    return this.selected();
  }
  get contentType() {
    return this.type();
  }
  choose(item: ModerationItem) {
    this.select(item);
  }
  setReason(reason: string) {
    this.form.controls.reason.setValue(reason);
  }
  confirm() {
    this.submit();
  }
  showBlocked() {
    this.filter.set('blocked');
  }
}

let auth: AuthService;
let forum: ForumService;
const route = { snapshot: { queryParamMap: convertToParamMap({}) } };
const input = {
  title: 'Una conversación nueva',
  categoryId: 'tecnologia',
  content: 'Una idea para conversar',
};
const login = (role = 'usuario') => auth.login(role + '@nexo.demo', 'NexoDemo2026!');
beforeEach(() => {
  vi.stubGlobal('crypto', webcrypto);
  route.snapshot.queryParamMap = convertToParamMap({});
  TestBed.configureTestingModule({
    providers: [
      FormBuilder,
      { provide: Router, useValue: { createUrlTree: (commands: string[]) => commands[0] } },
      { provide: ActivatedRoute, useValue: route },
    ],
  });
  auth = TestBed.inject(AuthService);
  forum = TestBed.inject(ForumService);
});
afterEach(() => vi.unstubAllGlobals());

it('permite lectura pública y exige sesión para publicar, comentar y moderar', () => {
  expect(forum.topics()).toHaveLength(5);
  expect(forum.topics().every((topic) => topic.replies === 0)).toBe(true);
  expect(forum.topic(999)).toBeUndefined();
  expect(() => forum.publish(input)).toThrow('Inicia sesión');
  expect(() => forum.comment(1, 'Comentario')).toThrow('Inicia sesión');
  expect(() => forum.moderationItems()).toThrow('Inicia sesión');
});
it('el autor publica, edita y elimina con contadores reales', async () => {
  await login();
  const id = forum.publish({ ...input, title: '  Nuevo tema  ' });
  expect(forum.topic(id)?.title).toBe('Nuevo tema');
  expect(forum.topic(id)?.authorId).toBe(auth.user()?.id);
  expect(forum.topics()[0].id).toBe(id);
  forum.updateTopic(id, { ...input, title: 'Tema actualizado' });
  const comment = forum.comment(id, '  Respuesta  ');
  expect(forum.comments(id)[0].content).toBe('Respuesta');
  expect(forum.topics()[0].replies).toBe(1);
  expect(() => forum.deleteTopic(id)).toThrow('tiene comentarios');
  forum.updateComment(comment, 'Respuesta editada');
  expect(forum.comments(id)[0].content).toBe('Respuesta editada');
  forum.deleteComment(comment);
  expect(forum.topics()[0].replies).toBe(0);
  forum.deleteTopic(id);
  expect(forum.topic(id)).toBeUndefined();
});
it('otro usuario y ADMIN no modifican contenido ajeno', async () => {
  await login();
  const id = forum.publish(input);
  const comment = forum.comment(id, 'Contenido');
  await auth.register('Otra Persona', 'otra@correo.cl', 'OtraClave2026!');
  for (const role of ['otro', 'admin']) {
    if (role === 'admin') await login('admin');
    expect(forum.canEdit(forum.topic(id)!)).toBe(false);
    expect(() => forum.updateTopic(id, input)).toThrow('Solo el autor');
    expect(() => forum.deleteTopic(id)).toThrow('Solo el autor');
    expect(() => forum.updateComment(comment, 'Ajeno')).toThrow('Solo el autor');
    expect(() => forum.deleteComment(comment)).toThrow('Solo el autor');
  }
});
it('solo ADMIN bloquea; restaurar el tema conserva los bloqueos de comentarios', async () => {
  await login();
  const id = forum.publish(input);
  const comment = forum.comment(id, 'Contenido');
  expect(() => forum.moderate('topic', id, true, 'Normas')).toThrow('ADMIN');
  expect(() => forum.moderationItems()).toThrow('ADMIN');
  await login('admin');
  forum.moderate('comment', comment, true, 'Spam');
  forum.moderate('topic', id, true, 'Revisión');
  expect(forum.topic(id)).toBeUndefined();
  expect(forum.comments(id)).toEqual([]);
  expect(forum.topics().some((topic) => topic.id === id)).toBe(false);
  expect(forum.moderationItems().find((item) => item.type === 'comment')?.parentBlocked).toBe(true);
  await login();
  expect(() => forum.updateTopic(id, input)).toThrow('no está disponible');
  expect(() => forum.comment(id, 'Intento')).toThrow('no está disponible');
  expect(() => forum.updateComment(comment, 'Intento')).toThrow('no está disponible');
  await login('admin');
  forum.moderate('topic', id, false, 'Aprobado');
  expect(forum.topic(id)?.moderatorId).toBe(auth.user()?.id);
  expect(forum.comments(id)).toEqual([]);
  forum.moderate('comment', comment, false, 'Restaurado');
  expect(forum.comments(id)).toHaveLength(1);
  expect(forum.topics().find((topic) => topic.id === id)?.replies).toBe(1);
});
it('el autor no puede editar ni borrar un comentario bloqueado', async () => {
  await login();
  const id = forum.comment(1, 'Respuesta');
  await login('admin');
  forum.moderate('comment', id, true, 'Normas');
  await login();
  expect(() => forum.updateComment(id, 'Intento')).toThrow('Solo el autor');
  expect(() => forum.deleteComment(id)).toThrow('Solo el autor');
});
it('rechaza contenido vacío, tamaños y categorías inválidas', async () => {
  await login();
  for (const value of [
    { ...input, title: ' ' },
    { ...input, title: 'x'.repeat(151) },
    { ...input, content: ' ' },
    { ...input, content: 'x'.repeat(10001) },
    { ...input, categoryId: 'no-existe' },
  ])
    expect(() => forum.publish(value)).toThrow();
  expect(() => forum.comment(1, ' ')).toThrow();
  expect(() => forum.comment(1, 'x'.repeat(5001))).toThrow();
  expect(() => forum.comment(999, 'Contenido')).toThrow();
  await login('admin');
  expect(() => forum.moderate('topic', 1, true, ' ')).toThrow();
  expect(() => forum.moderate('topic', 1, true, 'x'.repeat(501))).toThrow();
  expect(() => forum.moderate('topic', 999, true, 'Normas')).toThrow();
});
it('devuelve copias, conserva el texto y refleja el nombre del autor', async () => {
  await login();
  const text = '<script>alert(1)</script>';
  const id = forum.publish({ ...input, content: text });
  const copy = { ...forum.topic(id)! };
  copy.content = 'Alterado';
  expect(forum.topic(id)?.content).toBe(text);
  auth.updateProfile('Nuevo Nombre', 'usuario@nexo.demo', '');
  expect(forum.topic(id)?.author).toBe('Nuevo Nombre');
});
it('la ruta de moderación admite solo ADMIN', async () => {
  const guard = () =>
    TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  expect(guard()).toBe('/ingresar');
  await login();
  expect(guard()).toBe('/');
  await login('admin');
  expect(guard()).toBe(true);
});
it('mis publicaciones muestra propias, incluidas bloqueadas, y excluye otras', async () => {
  expect(forum.myTopics()).toEqual([]);
  await login();
  const id = forum.publish(input);
  forum.comment(id, 'Respuesta');
  expect(forum.myTopics()).toHaveLength(1);
  expect(forum.myTopics()[0].replies).toBe(1);
  await login('admin');
  expect(forum.myTopics()).toEqual([]);
  forum.moderate('topic', id, true, 'Normas');
  await login();
  expect(forum.myTopics()[0]).toMatchObject({ id, blocked: true, moderationReason: 'Normas' });
});
it('publicar conserva la sesión y las publicaciones sobreviven al cambio de cuenta', async () => {
  await login();
  const userId = auth.user()?.id;
  const id = forum.publish(input);
  expect(forum.topic(id)?.title).toBe(input.title);
  expect(auth.user()?.id).toBe(userId);
  auth.logout();
  expect(forum.myTopics()).toEqual([]);
  await login();
  expect(auth.user()?.id).toBe(userId);
  expect(forum.myTopics()[0].id).toBe(id);
});
it('el panel refleja el bloqueo, mensaje, filtros y restauración', async () => {
  await login('admin');
  const page = TestBed.runInInjectionContext(() => new ModerationPageTest());
  page.choose(page.viewItems.find((item) => item.type === 'topic' && item.id === 1)!);
  page.setReason('Normas');
  page.confirm();
  expect(page.statusMessage).toBe('Contenido bloqueado.');
  expect(page.decision).toBeNull();
  expect(page.viewItems.find((item) => item.type === 'topic' && item.id === 1)?.blocked).toBe(true);
  page.showBlocked();
  expect(page.viewItems).toHaveLength(1);
  page.choose(page.viewItems[0]);
  page.setReason('Aprobado');
  page.confirm();
  expect(page.statusMessage).toBe('Contenido restaurado.');
  expect(page.viewItems).toHaveLength(0);
});
it('el acceso desde comentario selecciona y bloquea únicamente ese comentario', async () => {
  await login();
  const id = forum.comment(1, 'Para moderar');
  const other = forum.comment(1, 'Visible');
  await login('admin');
  route.snapshot.queryParamMap = convertToParamMap({ tipo: 'comment', id: String(id) });
  const page = TestBed.runInInjectionContext(() => new ModerationPageTest());
  expect(page.decision).toMatchObject({ type: 'comment', id });
  expect(page.contentType).toBe('comment');
  page.setReason('Normas');
  page.confirm();
  expect(page.statusMessage).toBe('Contenido bloqueado.');
  expect(forum.topic(1)).toBeDefined();
  expect(forum.comments(1).map((comment) => comment.id)).toEqual([other]);
});
