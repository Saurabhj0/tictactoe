# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy the pom.xml and download dependencies (caching layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src src
RUN mvn package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built JAR from the build stage
# Using a wildcard to match the versioned jar name
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# The following environment variables can be overridden at runtime (e.g., in K8s)
# Defaults match the current development environment
ENV SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/tictactoe
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=Jlr@2025

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
