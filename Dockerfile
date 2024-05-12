FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY build/libs/Server-*-all.jar ./Server.jar
COPY run/* ./

EXPOSE 1234

ENTRYPOINT [ "java", "-jar", "Server.jar" ]