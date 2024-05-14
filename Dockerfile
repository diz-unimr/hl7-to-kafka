FROM gradle:8.7.0-jdk17 AS build
WORKDIR /home/gradle/src
ENV GRADLE_USER_HOME /gradle

COPY build.gradle settings.gradle ./
RUN gradle clean build --no-daemon > /dev/null 2>&1 || true

COPY --chown=gradle:gradle . .
RUN gradle build --info && \
    java -Djarmode=layertools -jar build/libs/*.jar extract

FROM gcr.io/distroless/java17:nonroot

USER nonroot
WORKDIR /opt/hl7-to-kafka
COPY --from=build /home/gradle/src/dependencies/ ./
COPY --from=build /home/gradle/src/spring-boot-loader/ ./
COPY --from=build /home/gradle/src/application/ ./
COPY HealthCheck.java .

ARG GIT_REF=""
ARG GIT_URL=""
ARG BUILD_TIME=""
ARG VERSION=0.0.0
ENV TZ="Europe/Berlin" APP_VERSION=${VERSION} \
    SPRING_PROFILES_ACTIVE="prod"
EXPOSE 8080 2575

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=90", "org.springframework.boot.loader.launch.JarLauncher"]

HEALTHCHECK --interval=25s --timeout=3s --retries=2 CMD ["java", "HealthCheck.java", "||", "exit", "1"]

LABEL org.opencontainers.image.created=${BUILD_TIME} \
    org.opencontainers.image.authors="Sebastian Stöcker" \
    org.opencontainers.image.source=${GIT_URL} \
    org.opencontainers.image.version=${VERSION} \
    org.opencontainers.image.revision=${GIT_REF} \
    org.opencontainers.image.vendor="diz.uni-marburg.de" \
    org.opencontainers.image.title="hl7-to-kafka" \
    org.opencontainers.image.description="Hl7 to Kafka producer"
