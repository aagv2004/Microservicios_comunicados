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

## Docker local

Requiere Docker Desktop con motor Linux y Docker Compose. Los Dockerfile compilan y ejecutan pruebas en una etapa de construcción; las imágenes finales incluyen solo el JAR y Java, o Angular compilado y Nginx.

Crear la carpeta `.docker-local` y dos archivos privados: `usuarios.properties` y `foro.properties`. Cada archivo debe contener los datos del esquema correspondiente:

```properties
spring.datasource.url=jdbc:oracle:thin:@ALIAS_ORACLE?TNS_ADMIN=/opt/oracle/wallet
spring.datasource.username=USUARIO_DEL_ESQUEMA
spring.datasource.password=CONTRASENA_LOCAL
spring.datasource.hikari.data-source-properties.oracle.net.tns_admin=/opt/oracle/wallet
```

Usar el alias TNS del Wallet y las credenciales de cada esquema. La ruta interna `/opt/oracle/wallet` corresponde al montaje de Compose; no es la ruta de Windows.

Copiar `.env.example` como `.env` y configurar `ORACLE_WALLET_PATH` con la ruta absoluta del Wallet en Windows. Si los archivos privados ya existen, se reutilizan. `.env`, `.docker-local` y el Wallet están excluidos de Git.

```powershell
docker compose config --quiet
docker compose build
docker compose up -d --wait --wait-timeout 240
docker compose ps
```

| Componente | Dirección desde Windows | Destino interno |
| --- | --- | --- |
| Frontend | `http://localhost:8088` | `frontend:80` |
| Usuarios | `http://localhost:8081` | `usuarios:8080` |
| Foro | `http://localhost:8082` | `foro:8080` |
| Moderación | `http://localhost:8083` | `moderacion:8080` |

Los puertos se publican solo en la interfaz local. Pueden cambiarse mediante las variables `FRONTEND_HOST_PORT`, `USUARIOS_HOST_PORT`, `FORO_HOST_PORT` y `MODERACION_HOST_PORT` de `.env`. Antes de arrancar, detener aplicaciones locales que ocupen esos mismos puertos.

El perfil `container` configura el puerto interno y las direcciones de otros servicios. Usuarios y Foro importan sus propiedades desde `/run/secrets/oracle.properties`; el Wallet se monta en `/opt/oracle/wallet` en modo lectura. Las imágenes no contienen esos archivos. La base de datos continúa en Oracle Cloud y debe estar disponible. Los perfiles locales y sus puertos se conservan.

Las comprobaciones de salud de los servicios Java verifican que el puerto interno esté abierto; no sustituyen una operación de lectura/escritura contra Oracle. Para probar las APIs desde Windows, usar las direcciones publicadas y la colección de `microservicio-moderacion/postman`. El frontend mantiene los datos simulados y funciona separado de las APIs. Nginx resuelve las rutas Angular mediante `index.html`, incluidas las aperturas directas de `/ingresar`, `/perfil` y `/temas/1`.

Consultar registros y detener exclusivamente este proyecto:

```powershell
docker compose logs --tail 80
docker compose down
```

`down` retira los contenedores y la red de Compose; no elimina los datos de Oracle ni el Wallet externo. No es necesario publicar imágenes en un registro para ejecutar esta configuración local.
