# --- Giai đoạn 1: Build (Thợ xây) ---
# Dùng Maven và Java 21 (Bản ổn định nhất hiện tại tương thích tốt với Spring Boot 3)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy file cấu hình và tải thư viện trước (để tận dụng cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy toàn bộ code vào và build
COPY src ./src
RUN mvn clean package -DskipTests

# --- Giai đoạn 2: Run (Chủ nhà) ---
# Dùng bản Java nhẹ để chạy cho nhanh
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Lấy cái file .jar đã build ở trên bỏ vào đây
COPY --from=build /app/target/*.jar app.jar

# Mở cổng 8080
EXPOSE 8080

# Lệnh chạy app
ENTRYPOINT ["java", "-jar", "app.jar"]