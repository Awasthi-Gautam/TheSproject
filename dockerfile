# Stage 1: Build the application using Gradle and Java 25
FROM gradle:jdk25 AS builder

# Copy project files and set working directory
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# Build the JAR (skipping tests for faster build; add --no-daemon if you see issues)
RUN gradle clean bootJar --no-daemon

# Stage 2: Create a slim runtime image with Java 25 JRE
FROM eclipse-temurin:25-jre

# Copy the built JAR from the builder stage
COPY --from=builder /home/gradle/src/build/libs/*.jar /app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "/app.jar"]