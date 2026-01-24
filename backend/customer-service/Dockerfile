# ===== build stage =====
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# ===== run stage =====
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 9000
ENTRYPOINT ["java","-jar","/app/app.jar"]