# Multi-stage build: compile the Java code first
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies (if any) to speed up subsequent builds
RUN mvn dependency:go-offline -B
# Copy source code and compile
COPY src ./src
RUN mvn package -DskipTests

# Final stage: minimal JRE image to run the compiled jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the compiled jar from the build stage
COPY --from=build /app/target/coresql-engine-1.0-SNAPSHOT.jar ./coresql.jar

# Create a directory for persistent tables data
RUN mkdir -p /app/tables
VOLUME ["/app/tables"]

# Expose the TCP server port
EXPOSE 5455

# Run the server by default
ENTRYPOINT ["java", "-jar", "coresql.jar", "--server"]
