# =========================
# BUILD STAGE
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom trước để cache dependency
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build jar
RUN mvn clean package -DskipTests


# =========================
# RUN STAGE
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy jar từ stage build
COPY --from=build /app/target/*.jar app.jar

# Expose port Spring Boot
EXPOSE 8080

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]
