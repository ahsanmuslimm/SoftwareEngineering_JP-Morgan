# Dockerfile for Midas Core
# Builds a containerized version of the Midas Core financial transaction service
# Base: OpenJDK 17 (JDK version used in pom.xml)

FROM openjdk:17-slim

# Metadata labels
LABEL maintainer="JP Morgan Chase"
LABEL description="Midas Core - Financial Transaction Processing Service (Full Fledged Working Prototype)"
LABEL version="0.0.1"

# Set working directory in container
WORKDIR /app

# Copy the built JAR from the Maven build (target/ directory)
# Expected: forage-midas-0.0.1-SNAPSHOT.jar (see pom.xml for artifact name)
COPY target/forage-midas-0.0.1-SNAPSHOT.jar app.jar

# Expose the service port (configured in application.yml: server.port=33400)
EXPOSE 33400

# Health check - verify the service is alive
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.JarLauncher --health || exit 1

# Default environment variables (can be overridden at runtime)
ENV SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb
ENV SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect
ENV MIDAS_API_KEY=midas-dev-key-2026

# Entry point: start the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
