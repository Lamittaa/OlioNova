FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS=""
COPY --from=build /app/target/ai-prediction-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9050
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
