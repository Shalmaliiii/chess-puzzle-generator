FROM eclipse-temurin:25-jdk

LABEL authors="Shalmali"

WORKDIR /app

# install stockfish
RUN apt-get update \
    && apt-get install -y --no-install-recommends stockfish \
    && rm -rf /var/lib/apt/lists/*

# Create and run as a non-root user so the container does not have root
# privileges at runtime.
RUN groupadd --system app && useradd --system --gid app --home-dir /app app

COPY --chown=app:app build/libs/puzzle-service.jar app.jar

USER app

ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=docker"]
