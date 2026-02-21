# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy the mvnw and .mvn directory to download dependencies first (caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Ensure mvnw has executable permissions
RUN chmod +x mvnw

# Download dependencies (caching layer)
RUN ./mvnw dependency:go-offline -B

# Copy the source code and build the application
COPY src src
RUN ./mvnw package -DskipTests

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
