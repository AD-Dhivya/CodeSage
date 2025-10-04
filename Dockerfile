
# Use a slim, stable OpenJDK 21 image
FROM openjdk:21-jdk-slim

# Metadata for judges
LABEL maintainer="you"
LABEL org.opencontainers.image.title="CodeSage - AI Code Mentor"
LABEL org.opencontainers.image.description="Java 21 + Spring Boot + Cerebras AI"
LABEL org.opencontainers.image.version="1.0.0"

# Set working directory
WORKDIR /app

# Install curl for health checks and debugging
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Copy Maven wrapper and POM first (for better layer caching)
COPY mvnw ./
COPY .mvn .mvn/
COPY pom.xml ./

# Make mvnw executable
RUN chmod +x ./mvnw

# Copy source code
COPY src ./src

# Build the project (batch mode for Docker)
# Skip tests to speed up build
RUN ./mvnw clean package -DskipTests -B

# Optional: Extract layers (improves rebuild speed in future)
# RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../CodeSage-1.0.0.jar)

# Expose port 8080 (Spring Boot default)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "target/CodeSage-1.0.0.jar"]
