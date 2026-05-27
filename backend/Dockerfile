# ───────────────────────────────────────────────────────────
# CreatorEngine backend — multi-stage Docker build
# ───────────────────────────────────────────────────────────

# ─── Stage 1: build the fat jar ───────────────────────────
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy everything and build in one step. Skipping the
# `dependency:go-offline` optimisation because it doesn't reliably
# cache annotation processors (Lombok), which caused builds to fail
# with "cannot find symbol getId()" type errors.
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# ─── Stage 2: minimal runtime ─────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider "http://localhost:${PORT:-8080}/api/health" || exit 1

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]