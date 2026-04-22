FROM eclipse-temurin:25-jdk

LABEL authors="Shalmali"

WORKDIR /app

# install stockfish
RUN apt-get update \
    && apt-get install -y --no-install-recommends stockfish \
    && rm -rf /var/lib/apt/lists/*

COPY build/libs/puzzle-service.jar app.jar

# Run as an unprivileged user rather than root.
RUN groupadd --system app && useradd --system --gid app --home-dir /app app \
    && chown -R app:app /app
USER app

ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=docker"]
