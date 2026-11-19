# Nexo — Frontend del foro

Aplicación Angular 21 con Bootstrap 5.3 y datos simulados en memoria.

## Ejecutar

```powershell
npm ci
npm start
```

Abrir `http://localhost:4200`. `npm run build` genera la compilación de producción y `npm test` ejecuta las pruebas de sesión y validadores.

## Rutas

| Ruta | Función |
| --- | --- |
| `/` | Categorías, búsqueda y conversaciones de ejemplo |
| `/ingresar` | Inicio de sesión |
| `/registro` | Registro de usuario |
| `/recuperar` | Solicitud de recuperación simulada |
| `/restablecer?token=...` | Restablecimiento mediante enlace temporal |
| `/perfil` | Edición de nombre, correo y biografía; requiere sesión |

## Cuentas de demostración

- USUARIO: `usuario@nexo.demo`
- ADMIN: `admin@nexo.demo`
- Contraseña para ambas: `NexoDemo2026!`

El registro siempre crea usuarios de rol USUARIO. La edición de perfil no permite modificar el rol. Las cuentas, cambios y sesión existen solo en memoria; una recarga restaura los datos iniciales. No se almacenan contraseñas ni sesiones en localStorage o sessionStorage. La contraseña se deriva con PBKDF2 y una sal aleatoria dentro de esta simulación.

## Validaciones

Nombre de 3 a 100 caracteres, correo válido de hasta 150 caracteres y comprobación de correo duplicado. Las contraseñas de registro y recuperación requieren de 10 a 64 caracteres, mayúscula, minúscula, número, símbolo y ausencia de espacios. La confirmación debe coincidir. La biografía es opcional y admite hasta 280 caracteres.

Los formularios usan Reactive Forms y atributos HTML. Los errores aparecen al abandonar el campo o enviar el formulario. Las contraseñas pueden mostrarse u ocultarse. Se bloquea el envío repetido mientras una operación está en curso.

La recuperación no envía correos: el enlace se presenta en la pantalla como simulación. Vence en 15 minutos, solo admite un uso y una nueva solicitud invalida el anterior. Este mecanismo local no representa autorización de un backend real.

## Estructura

- `core/services/auth.service.ts`: cuentas y sesión simuladas.
- `core/services/forum.service.ts`: categorías y temas de ejemplo.
- `core/guards/session.guard.ts`: acceso al perfil.
- `shared/forms`: campos compartidos y validadores.
- `features/account`: formularios de cuenta cargados bajo demanda.
- `features/forum`: portada, búsqueda y filtros.
- `tests/auth.test.mjs`: pruebas con el ejecutor incorporado de Node.

## Diseño responsive

Grid Bootstrap de 12 columnas. En la portada, móvil usa una columna; tablet, distribución 4+8; escritorio, 3+9. Los formularios apilan introducción y campos en móvil y los distribuyen en columnas desde tablet. Anchos de revisión: 390, 768 y 1440 píxeles.

## Alcance

El frontend funciona separado de las APIs. Publicación de temas, gestión de comentarios y panel de moderación están pendientes. Los roles del navegador solo controlan la simulación; la autorización real pertenece a los microservicios.
