# Chọn image chính thức Playwright Java với Ubuntu focal (đầy đủ dependency)
FROM mcr.microsoft.com/playwright/java:v1.49.0-focal

# Thiết lập thư mục làm việc
WORKDIR /app

# Copy toàn bộ source code Maven vào container
COPY . .

# Cấp quyền thực thi cho Maven Wrapper (nếu cần)
RUN chmod +x mvnw

# Build project Maven, bỏ qua test
RUN ./mvnw clean package -DskipTests

# Thiết lập biến môi trường PORT cho Railway/Fly.io
ENV PORT=8080
EXPOSE 8080

# Chạy file JAR vừa build
CMD ["java", "-jar", "target/schedule-0.0.1-SNAPSHOT.jar"]
