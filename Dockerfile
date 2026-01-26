# --- Giai đoạn 1: Build (Thợ xây) ---
# Dùng Maven và Java 17 (khớp với pom.xml)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy file cấu hình và tải thư viện trước (để tận dụng cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy toàn bộ code vào và build
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Giai đoạn 2: Run (Chủ nhà) ---
# Dùng bản Java nhẹ để chạy cho nhanh
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Tạo user non-root để chạy app (security best practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Lấy cái file .jar đã build ở trên bỏ vào đây
COPY --from=build /app/target/*.jar app.jar

# Mở cổng (Render sẽ dùng PORT env variable, mặc định 8080)
EXPOSE 8080

# Health check (optional, để Render biết app đã sẵn sàng)
# Sử dụng wget (cần cài đặt trong alpine) hoặc có thể bỏ qua nếu không cần
# HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
#   CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/api/auth/status || exit 1

# Lệnh chạy app (Render sẽ tự động set PORT env variable)
ENTRYPOINT ["java", "-jar", "app.jar"]