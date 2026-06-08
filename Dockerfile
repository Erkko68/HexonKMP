# ── Build stage ────────────────────────────────────────────────────────────────
# JDK 22: core/app are pinned to jvmToolchain(22)/JVM_22, so the build (and the
# runtime below) need Java 22. Using a 22 base means Gradle's toolchain matches
# the running JDK — no JDK is auto-downloaded during the build.
FROM eclipse-temurin:22-jdk AS build
WORKDIR /build

# The Android Gradle Plugin requires sdk.dir during project configuration even
# when we only compile the JVM :server target.  A stub empty directory satisfies
# the check without pulling in the full SDK.
ENV ANDROID_HOME=/tmp/android-sdk
RUN mkdir -p /tmp/android-sdk
COPY . .
# gradle-daemon-jvm.properties pins the daemon to Amazon Corretto 21 (a local-dev
# artifact); remove it so the daemon runs on this image's JDK 22 instead of trying
# to provision Corretto. Build with the project's wrapper (Gradle 9.1.0), not a
# mismatched system gradle.
RUN rm -f gradle/gradle-daemon-jvm.properties && \
    printf "sdk.dir=/tmp/android-sdk\n" > local.properties && \
    chmod +x ./gradlew && \
    ./gradlew :server:buildFatJar --no-daemon

# ── Runtime stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:22-jre-alpine
WORKDIR /app
COPY --from=build /build/server/build/libs/server-*-all.jar server.jar

# SERVER_BIND_HOST / SERVER_BIND_PORT are read by application.conf at runtime.
# Override them via docker run -e or docker-compose environment.
ENV SERVER_BIND_HOST=0.0.0.0
ENV SERVER_BIND_PORT=8080

EXPOSE 8080
CMD ["java", "-jar", "server.jar"]
