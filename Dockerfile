# ============================================================
# Etapa 1: Build - compila el .jar con Maven
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Se copian primero los descriptores de dependencias para aprovechar
# la cache de capas de Docker: si el pom.xml no cambia, no se vuelven
# a descargar todas las dependencias en cada build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ============================================================
# Etapa 2: Runtime - imagen final liviana, sin herramientas de build
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Buenas practicas de seguridad: ejecutar el contenedor con un usuario
# no-root en lugar del usuario root por defecto de la imagen.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /build/target/api-0.0.1-SNAPSHOT.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
