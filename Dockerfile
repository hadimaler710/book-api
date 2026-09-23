# syntax=docker/dockerfile:1

# =============================================================================
# Multi-stage build for the Book Catalogue API.
# Build:  docker build -t book-api .
# Run:    docker run -p 8080:8080 book-api
# The container runs with the prod profile (file-based H2 at /app/data).
# =============================================================================

# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

# Cache dependencies first: re-copying only src/ then rebuilds fast
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN ./mvnw -q -B dependency:go-offline

COPY src ./src
RUN ./mvnw -q -B package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Prod profile stores the H2 file DB in a writable volume
RUN mkdir -p /app/data && chown -R spring:spring /app/data
VOLUME ["/app/data"]

COPY --from=build /workspace/app/target/book-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    BOOK_API_DATASOURCE_URL=jdbc:h2:file:/app/data/bookdb

HEALTHCHECK --interval=30s --timeout=3s --start-period=25s \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
