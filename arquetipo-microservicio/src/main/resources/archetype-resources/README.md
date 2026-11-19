# ${artifactId}

Generado con `com.example.foro:arquetipo-microservicio:1.0.0`.
Paquete Java: `${package}`. Java 21, Spring Boot 4.1.2.

## Comprobar

`mvn test` ejecuta pruebas HTTP con H2: estado público con respuesta JSON 200 y bloqueo de otras rutas con 403.
`mvn package` ejecuta las pruebas y genera un JAR ejecutable.

## Ejecutar contra Oracle

Configurar en el entorno `ORACLE_SERVICE`, `TNS_ADMIN` (carpeta externa del Wallet), `ORACLE_USERNAME` y `ORACLE_PASSWORD`.
Opcional: `SERVICE_NAME` y `SERVER_PORT` (por defecto microservicio y 8083).
Ejecutar `mvn spring-boot:run`. GET `/api/estado` devuelve JSON con el nombre del artefacto.
Hibernate valida el esquema y no crea ni elimina tablas en Oracle.

## Responsabilidades

- controller: recibe HTTP y delega.
- service: lógica del negocio.
- repository: acceso JPA a datos.
- model: entidades propias.
- dto: contratos de entrada/salida.
- config: configuración, incluida seguridad.
- exception: errores del servicio.

Las capas sin implementación incluyen package-info.java.
Solo el endpoint de estado es público; el resto está denegado hasta implementar la autorización real con Usuarios.
El estado OK indica que responde la aplicación, no una prueba de salud de Oracle.
No incluye credenciales, Wallet ni funcionalidades de moderación.
