export interface User {
  id: string;
  nombre: string;
  correo: string;
  biografia: string;
  rol: 'USUARIO' | 'ADMIN';
}
