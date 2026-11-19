# Arquetipo común de microservicios

Coordenadas: `com.example.foro:arquetipo-microservicio:1.0.0`.

Plantilla de microservicios con Java 21, Spring Boot 4.1.2, Maven, Web MVC, validación, Spring Security, JPA, controlador Oracle y H2 para pruebas.

## Qué genera

```text
src/main/java/<paquete>/
  MicroservicioApplication.java
  controller/EstadoController.java
  service/EstadoService.java
  dto/EstadoResponse.java
  config/SecurityConfig.java
  model/package-info.java
  repository/package-info.java
  exception/package-info.java
src/main/resources/application.properties
src/test/java/<paquete>/MicroservicioApplicationTests.java
src/test/resources/application-test.properties
pom.xml
.gitignore
README.md
```

Los archivos package-info.java documentan las capas aún vacías y permiten conservarlas en Git y en la generación sin crear entidades ficticias.

## Requisitos

Java 21 y Maven disponible como `mvn.cmd` en Windows. Verificar `mvn.cmd --version`.
`JAVA_HOME` debe apuntar a un JDK 21 válido.

## Generar un servicio nuevo

Primero, desde esta carpeta:

```powershell
mvn.cmd -B install
```

Desde la carpeta de destino (por ejemplo, `Microservicios`):

```powershell
mvn.cmd -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate `
  '-DarchetypeGroupId=com.example.foro' `
  '-DarchetypeArtifactId=arquetipo-microservicio' `
  '-DarchetypeVersion=1.0.0' `
  '-DarchetypeCatalog=local' `
  '-DgroupId=com.example.foro' `
  '-DartifactId=microservicio-moderacion' `
  '-Dversion=0.0.1-SNAPSHOT' `
  '-Dpackage=com.example.foro.servicio_moderacion' `
  '-DinteractiveMode=false'
```

El ejemplo genera la base de Moderación; no su funcionalidad. No ejecutar sobre una carpeta de servicio ya existente.

## Configuración generada

Oracle se configura con `ORACLE_SERVICE`, `TNS_ADMIN`, `ORACLE_USERNAME` y `ORACLE_PASSWORD` del entorno. La configuración se copia sin filtrado Velocity: las variables de Spring llegan intactas. Hibernate usa `validate`, sin reconstrucción automática de tablas. El perfil `test` sobrescribe la conexión con H2.

`SERVER_PORT` permite cambiar el puerto (8083 por defecto). `SERVICE_NAME` cambia el nombre de Spring; el JSON del endpoint identifica el artifactId generado.

Solo `/api/estado` es público. Las rutas de negocio requieren implementar autenticación y autorización. El estado OK demuestra respuesta de la aplicación, no salud de Oracle.
