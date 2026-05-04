# Stage 1: Build the JAR with forced Java 21 targeting
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom first to cache dependencies
COPY pom.xml .
COPY src ./src

# The -Dmaven.compiler.release=21 flag is the "magic" fix here
RUN mvn clean package -DskipTests -Dmaven.compiler.release=21

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the generated JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]