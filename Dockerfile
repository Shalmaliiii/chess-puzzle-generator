FROM eclipse-temurin:25-jdk

LABEL authors="Shalmali"

WORKDIR /app

# install stockfish
RUN apt-get update && apt-get install -y stockfish

COPY build/libs/puzzle-service.jar app.jar

ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=docker"]