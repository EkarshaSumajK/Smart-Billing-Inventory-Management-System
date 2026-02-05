# Use Java 17 (Render supports this)
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .

# Ensure wrapper is executable
RUN chmod +x mvnw

# Build the JAR (skip tests for faster render builds)
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

# Run the Spring Boot JAR
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
