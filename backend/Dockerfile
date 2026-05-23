# ───────────────────────────────────────────────────────────
# CreatorEngine backend — multi-stage Docker build
# ───────────────────────────────────────────────────────────

# ─── Stage 1: build the fat jar ───────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache the dependency layer separately from the source so iterative
# rebuilds skip the long `mvn dependency:go-offline` step when only
# Java files change.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ─── Stage 2: minimal runtime ─────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as a non-root user — container best practice.
RUN addgroup -S app && adduser -S app -G app
USER app

# The Spring Boot fat jar; the wildcard avoids hard-coding the version.
COPY --from=build /app/target/*.jar app.jar

# Render injects PORT; Spring is configured to honour it.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check — calls the public /api/health endpoint inside the
# container. Render also probes healthCheckPath via the public URL.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider "http://localhost:${PORT:-8080}/api/health" || exit 1

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
