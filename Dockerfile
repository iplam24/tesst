# Sử dụng image Playwright Java với Ubuntu 20.04, đã có JDK 21 và đầy đủ dependency
FROM mcr.microsoft.com/playwright/java:v1.49.0-focal

# Thiết lập thư mục làm việc
WORKDIR /app

# Copy toàn bộ source code Maven vào container
COPY . .

# Build dự án Maven, bỏ qua test để nhanh hơn
RUN ./mvnw clean package -DskipTests

# Thiết lập biến môi trường PORT cho Railway
ENV PORT=8080
EXPOSE 8080

# Chạy file JAR sau khi build
CMD ["java", "-jar", "target/schedule-0.0.1-SNAPSHOT.jar"]
