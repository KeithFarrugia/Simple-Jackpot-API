# =============================================================================
# Here we are creating a maven image to build the project.
# This way building the project is self contained and easy to move
# =============================================================================

FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

# =============================================================================
# This is the actual container that runs the image. This way the overall
# container size is kept small.
# =============================================================================

FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]