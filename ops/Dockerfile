FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && useradd --uid 10001 --create-home app
WORKDIR /app
ARG SERVICE
COPY ${SERVICE}/target/${SERVICE}-1.0.0-SNAPSHOT.jar /app/app.jar
USER 10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java","-jar","/app/app.jar"]
