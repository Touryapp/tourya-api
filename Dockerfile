# Usa una imagen base de Maven para construir el proyecto
FROM maven:3.9.4-eclipse-temurin-17 AS build

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia los archivos del proyecto al contenedor
COPY pom.xml .
COPY src ./src

# Ejecuta el build del proyecto usando Maven
RUN mvn clean package -DskipTests

# Usa una imagen base de OpenJDK para ejecutar la aplicación
FROM eclipse-temurin:17-jre-jammy

# TC-020 #235 bug (c): Cloud Run defaults to UTC. Set container/JVM tz to America/Bogota
# so that every LocalDateTime.now() bare (~28 sites across services + jobs) resolves to
# Colombia local time. Install tzdata so /usr/share/zoneinfo has the zone, then export TZ.
RUN apt-get update && apt-get install -y --no-install-recommends tzdata && rm -rf /var/lib/apt/lists/*
ENV TZ=America/Bogota

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia el archivo JAR generado en la etapa de construcción
COPY --from=build /app/target/*SNAPSHOT.jar app.jar

# Expone el puerto en el que la aplicación se ejecutará
EXPOSE 8088

# Comando para ejecutar la aplicación (-Duser.timezone respalda a TZ para el JVM)
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=America/Bogota", "-jar", "/app/app.jar"]
