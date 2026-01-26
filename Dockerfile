# --- Giai đoạn 1: Build ---
# 👇 Dùng Java 21 cho nó khớp với Spring Boot 3 hiện tại
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
# Tải dependency trước để tận dụng cache của Docker (build lần sau nhanh hơn)
RUN mvn dependency:go-offline -B

COPY src ./src
# Build ra file .jar (skip test cho nhanh)
RUN mvn clean package -DskipTests -B

# --- Giai đoạn 2: Run ---
# 👇 Cũng phải là Java 21 (Alpine cho nhẹ)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tạo user để bảo mật (Best practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy file .jar từ giai đoạn build sang giai đoạn run
# (Lấy file jar đầu tiên tìm thấy - thường là file app chính)
COPY --from=build /app/target/*.jar app.jar

# Cổng mặc định (Railway/Render sẽ tự override bằng biến môi trường PORT)
ENV PORT=8080
EXPOSE 8080

# Chạy app
ENTRYPOINT ["java", "-jar", "app.jar"]