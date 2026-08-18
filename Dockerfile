FROM gradle:9.7.0-jdk25 AS build
WORKDIR /builder
ENV GRADLE_USER_HOME=/gradle

COPY build.gradle settings.gradle ./
RUN gradle clean build --no-daemon > /dev/null 2>&1 || true

COPY --chown=gradle:gradle . .
RUN gradle build --info && \
    java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher  --destination extracted && \
    javac HealthCheck.java

FROM gcr.io/distroless/java25:nonroot

USER nonroot
WORKDIR /opt/hl7-to-kafka
COPY --from=build /builder/extracted/dependencies/ ./
COPY --from=build /builder/extracted/spring-boot-loader/ ./
COPY --from=build /builder/extracted/snapshot-dependencies/ ./
COPY --from=build /builder/extracted/application/ ./
COPY --from=build /builder/HealthCheck.class .

ARG GIT_REF=""
ARG GIT_URL=""
ARG BUILD_TIME=""
ARG VERSION=0.0.0
ENV TZ="Europe/Berlin" APP_VERSION=${VERSION} \
    SPRING_PROFILES_ACTIVE="prod"
EXPOSE 8080 2575

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=90", "org.springframework.boot.loader.launch.JarLauncher"]

HEALTHCHECK --interval=25s --timeout=3s --retries=2 CMD ["java", "HealthCheck", "||", "exit", "1"]

LABEL org.opencontainers.image.created=${BUILD_TIME} \
    org.opencontainers.image.authors="Sebastian Stöcker" \
    org.opencontainers.image.source=${GIT_URL} \
    org.opencontainers.image.version=${VERSION} \
    org.opencontainers.image.revision=${GIT_REF} \
    org.opencontainers.image.vendor="diz.uni-marburg.de" \
    org.opencontainers.image.title="hl7-to-kafka" \
    org.opencontainers.image.description="Hl7 to Kafka producer"
