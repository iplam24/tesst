# Bước 1: Sử dụng Image chính thức của Microsoft Playwright cho Java
# Image này đã bao gồm JDK và tất cả thư viện cần thiết cho trình duyệt chạy trên Linux
FROM mcr.microsoft.com/playwright/java:v1.49.0-noble

# Thiết lập thư mục làm việc trong container
WORKDIR /app

# Copy toàn bộ mã nguồn vào thư mục làm việc
COPY . .

# Cấp quyền thực thi cho Maven Wrapper (Khắc phục lỗi Permission Denied)
RUN chmod +x mvnw

# Build dự án ra file JAR (Bỏ qua chạy test để build nhanh hơn)
RUN ./mvnw clean package -DskipTests

# Thiết lập biến môi trường cho cổng Port (Railway sẽ tự động gán giá trị này)
ENV PORT=8080
EXPOSE 8080

# Chạy ứng dụng Java
# LƯU Ý: Đảm bảo tên file .jar trong thư mục target/ khớp với tên dự án của bạn.
# Bạn có thể kiểm tra tên này trong file pom.xml (artifactId + version)
CMD ["java", "-jar", "target/schedule-0.0.1-SNAPSHOT.jar"]