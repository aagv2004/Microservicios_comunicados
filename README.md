# Sistema de foros

Repositorio con frontend Angular y microservicios Spring Boot independientes.

| Proyecto | Función | Puerto predeterminado |
| --- | --- | --- |
| `frontend-foro` | Interfaz Angular con datos simulados | 4200 |
| `microservicio_usuarios` | Cuentas, autenticación y perfiles | 8081 |
| `microservicio_foro` | Categorías, temas y comentarios | 8082 |
| `microservicio-moderacion` | Bloqueo y restauración por ADMIN | 8083 |
| `arquetipo-microservicio` | Plantilla generadora Maven | — |

## Configuración Oracle

Usuarios y Foro requieren Java 21, acceso a Oracle Cloud y Wallet externo al repositorio. Antes de iniciar cada servicio, configurar `ORACLE_SERVICE`, `TNS_ADMIN`, `ORACLE_USERNAME` y `ORACLE_PASSWORD` en el entorno.

Como alternativa local, cada carpeta de servicio puede contener `application-local.properties`, ignorado por Git:

```properties
spring.datasource.url=jdbc:oracle:thin:@ALIAS_ORACLE?TNS_ADMIN=C:/ruta/externa/Wallet
spring.datasource.username=USUARIO_DEL_ESQUEMA
spring.datasource.password=CONTRASENA_LOCAL
spring.datasource.hikari.data-source-properties.oracle.net.tns_admin=C:/ruta/externa/Wallet
```

El archivo local sobrescribe esas propiedades de `application.properties`. Iniciar desde la carpeta del servicio para que se encuentre mediante `spring.config.import`. No ubicar el archivo privado ni el Wallet dentro de `src/main/resources`, porque se incluirían en el JAR.

Hibernate valida el esquema existente. La migración de moderación se encuentra en `microservicio_foro/migraciones`; los scripts base de reconstrucción no deben ejecutarse sobre datos que deban conservarse.

## Comandos

Desde cada carpeta backend:

```powershell
mvn.cmd verify
mvn.cmd spring-boot:run
```

Desde `frontend-foro`:

```powershell
npm ci
npm test
npm start
```

Los README de cada proyecto detallan configuración y endpoints. El frontend de esta etapa no consume las APIs. Los archivos privados de configuración, Wallet, dependencias instaladas, logs y compilaciones están excluidos mediante `.gitignore`.
