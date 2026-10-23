FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY catalog-service/pom.xml catalog-service/
RUN --mount=type=cache,target=/root/.m2 mvn -B -f catalog-service/pom.xml dependency:go-offline
COPY catalog-service/src/ catalog-service/src/
RUN --mount=type=cache,target=/root/.m2 mvn -B -f catalog-service/pom.xml package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/catalog-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
