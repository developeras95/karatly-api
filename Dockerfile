# syntax=docker/dockerfile:1
#
# Karatly API as a container image. Needs only Docker: Java and Maven run inside the build.
#   docker build -t karatly-api .
#   docker compose --profile app up --build     (app + Postgres)
#
# Tests are not run here: the integration tests start their own PostgreSQL with Testcontainers,
# which needs Docker inside the build. CI (./mvnw verify) runs every test before code reaches main.

# ---- 1. Build the jar -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace

# Maven wrapper and pom first: this layer (and the downloaded dependencies) is reused until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode --no-transfer-progress -DskipTests package

# Split the fat jar into layers that change at different speeds (libraries rarely, our code often).
RUN cp target/karatly-api-*.jar application.jar \
 && java -Djarmode=tools -jar application.jar extract --layers --destination extracted \
 && mkdir -p extracted/dependencies extracted/spring-boot-loader extracted/snapshot-dependencies extracted/application

# ---- 2. Runtime image -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-noble

LABEL org.opencontainers.image.title="Karatly API" \
      org.opencontainers.image.description="Precious metal holdings, purchases, sales and true-profit analysis" \
      org.opencontainers.image.source="https://github.com/developeras95/karatly-api" \
      org.opencontainers.image.licenses="LicenseRef-Proprietary"

# Run as an unprivileged user, never as root.
RUN groupadd --system karatly && useradd --system --gid karatly --no-create-home karatly

WORKDIR /application
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
COPY LICENSE ./

USER karatly
EXPOSE 8081

# Size the heap from the container's memory limit instead of the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "application.jar"]
