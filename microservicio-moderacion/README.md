# Microservicio de Moderación

Servicio Spring Boot 4.1.2 / Java 21 generado con `com.example.foro:arquetipo-microservicio:1.0.0`.

## Arquitectura

Moderación valida la identidad con `GET /api/auth/me` de Usuarios y exige rol ADMIN. Reenvía la operación y las credenciales HTTP Basic a Foro, que vuelve a autorizarla y persiste el estado. No almacena copias del contenido ni mantiene una base propia; las dependencias de persistencia generadas por el arquetipo se retiraron de este servicio.

## Configuración

| Variable | Valor predeterminado |
| --- | --- |
| `SERVER_PORT` | `8083` |
| `USUARIOS_BASE_URL` | `http://localhost:8081` |
| `FORO_BASE_URL` | `http://localhost:8082` |

Los servicios deben ser accesibles entre sí. En un despliegue público, las credenciales HTTP Basic requieren transporte HTTPS.

## Ejecutar y probar

Con Java 21 y Maven:

```powershell
mvn.cmd verify
mvn.cmd spring-boot:run
```

Las pruebas usan servidores HTTP locales simulados para Usuarios y Foro. No requieren Oracle. `GET /api/estado` es público y verifica respuesta de la aplicación; no comprueba disponibilidad de sus dependencias.

## API

Ambas operaciones requieren HTTP Basic de un ADMIN:

- `PUT /api/moderacion/temas/{id}`
- `PUT /api/moderacion/comentarios/{id}`

```json
{
  "bloqueado": true,
  "motivo": "Incumplimiento de las normas del foro"
}
```

`bloqueado` es obligatorio; `false` restaura el contenido. El motivo es obligatorio, no puede estar vacío y admite hasta 500 caracteres. El identificador debe ser positivo. El administrador responsable se obtiene de Usuarios, no del JSON enviado por el cliente.

Respuesta de ejemplo:

```json
{
  "tipo": "TEMA",
  "id": 1,
  "bloqueado": true,
  "motivo": "Incumplimiento de las normas del foro",
  "moderadorId": 100
}
```

Errores: 400 datos inválidos; 401 credenciales ausentes o inválidas; 403 rol insuficiente; 404 contenido inexistente; 409 conflicto de actualización; 503 dependencia no disponible. Los cuerpos de error externos no se exponen al cliente.

## Persistencia en Foro

Antes de ejecutar la nueva versión de Foro contra un esquema existente, aplicar una sola vez `../microservicio_foro/migraciones/002_moderacion.sql` con el propietario de sus tablas. En una instalación nueva, ejecutar el script base de Foro y después esta migración. La migración agrega columnas sin borrar contenido. No debe repetirse si las columnas ya existen.

Foro conserva el texto, la autoría, el estado de bloqueo, el motivo y el ID del último administrador responsable. Un tema bloqueado y todos sus comentarios quedan fuera de la lectura pública; un comentario bloqueado se oculta individualmente. Las consultas directas de contenido bloqueado devuelven 404. La restauración de un tema conserva los bloqueos individuales de sus comentarios. Se mantienen las reglas de autoría para las operaciones ordinarias.

## Postman

Importar `postman/Moderacion.postman_collection.json`. Configurar las variables de la colección con cuentas y contenido existentes. Las credenciales incluidas son variables vacías; no se entregan cuentas reales.
