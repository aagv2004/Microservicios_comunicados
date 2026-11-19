import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
export const adminGuard: CanActivateFn = () => {
  const user = inject(AuthService).user();
  return user?.rol === 'ADMIN' ? true : inject(Router).createUrlTree([user ? '/' : '/ingresar']);
};
