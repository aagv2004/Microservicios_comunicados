import { Routes } from '@angular/router';
import { ForumPage } from './features/forum/forum-page';
import { adminGuard } from './core/guards/admin.guard';
import { sessionGuard } from './core/guards/session.guard';
const loadAccount = () =>
  import('./features/account/account-page').then((module) => module.AccountPage);

export const routes: Routes = [
  { path: '', component: ForumPage, title: 'Nexo | Conversaciones que conectan' },
  {
    path: 'ingresar',
    loadComponent: loadAccount,
    data: { mode: 'login' },
    title: 'Iniciar sesión | Nexo',
  },
  {
    path: 'registro',
    loadComponent: loadAccount,
    data: { mode: 'register' },
    title: 'Crear cuenta | Nexo',
  },
  {
    path: 'recuperar',
    loadComponent: loadAccount,
    data: { mode: 'recovery' },
    title: 'Recuperar contraseña | Nexo',
  },
  {
    path: 'restablecer',
    loadComponent: loadAccount,
    data: { mode: 'reset' },
    title: 'Nueva contraseña | Nexo',
  },
  {
    path: 'perfil',
    loadComponent: loadAccount,
    data: { mode: 'profile' },
    canActivate: [sessionGuard],
    title: 'Mi perfil | Nexo',
  },
  {
    path: 'temas/nuevo',
    loadComponent: () =>
      import('./features/forum/topic-editor').then((module) => module.TopicEditor),
    canActivate: [sessionGuard],
    title: 'Nueva conversación | Nexo',
  },
  {
    path: 'temas/:id/editar',
    loadComponent: () =>
      import('./features/forum/topic-editor').then((module) => module.TopicEditor),
    canActivate: [sessionGuard],
    title: 'Editar conversación | Nexo',
  },
  {
    path: 'temas/:id',
    loadComponent: () =>
      import('./features/forum/topic-detail').then((module) => module.TopicDetail),
    title: 'Conversación | Nexo',
  },
  {
    path: 'moderacion',
    loadComponent: () =>
      import('./features/forum/moderation-page').then((module) => module.ModerationPage),
    canActivate: [adminGuard],
    title: 'Moderación | Nexo',
  },
  { path: '**', redirectTo: '' },
];
