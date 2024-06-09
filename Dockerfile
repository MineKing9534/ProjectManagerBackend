FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY build/libs/ProjectManagerBackend-*-all.jar ./Server.jar
COPY run_production/* ./

EXPOSE 1234

ENTRYPOINT [ "java", "-jar", "Server.jar" ]