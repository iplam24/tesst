# Sử dụng Image chính thức từ Microsoft đã cài sẵn JDK và mọi thư viện trình duyệt
FROM mcr.microsoft.com/playwright/java:v1.49.0-noble

# Thiết lập thư mục làm việc
WORKDIR /app

# Copy mã nguồn vào container
COPY . .

# Build dự án bằng Maven (sử dụng wrapper đi kèm dự án)
RUN ./mvnw clean package -DskipTests

# Cấu hình biến môi trường
ENV PORT=8080
EXPOSE 8080

# Chạy file jar sau khi build thành công
# Lưu ý: Thay 'schedule-0.0.1-SNAPSHOT.jar' bằng tên file jar thực tế trong thư mục target của bạn
CMD ["java", "-jar", "target/schedule-0.0.1-SNAPSHOT.jar"]