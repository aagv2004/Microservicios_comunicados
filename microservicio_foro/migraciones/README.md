# Migraciones de Foro

`002_moderacion.sql` agrega las columnas de moderación a `foro_temas` y `foro_comentarios` sin eliminar datos.

Aplicar una sola vez con el propietario de las tablas antes de iniciar Foro con la implementación de moderación. Para un esquema nuevo, ejecutar primero el script base `foro.sql` y luego esta migración. No volver a ejecutar el script base sobre datos que deban conservarse: contiene operaciones de reconstrucción.

Comprobar previamente si las columnas `BLOQUEADO`, `MOTIVO_BLOQUEO` y `MODERADOR_ID` existen en ambas tablas. Oracle realiza commit implícito en DDL; si la ejecución se interrumpe tras la primera sentencia, revisar qué tabla fue modificada antes de continuar.

La aplicación guarda 0/1 mediante `NumericBooleanConverter`, compatible con las columnas `NUMBER(1)`. Las pruebas automatizadas utilizan H2; no validan esta migración contra Oracle.
