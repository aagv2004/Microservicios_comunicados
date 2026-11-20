FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp verify

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build --chown=app:app /workspace/target/*.jar /app/app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=container
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=5s --start-period=90s --retries=5 CMD bash -c '</dev/tcp/127.0.0.1/8080' || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
