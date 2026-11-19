import { Routes } from '@angular/router';
import { ForumPage } from './features/forum/forum-page';
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
  { path: '**', redirectTo: '' },
];
