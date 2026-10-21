# Stage 1: Build source code với Maven JDK 21 Alpine
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy tệp cấu hình Maven và wrapper
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Xử lý ký tự xuống dòng (CRLF sang LF nếu chạy trên Windows) và cấp quyền thực thi cho mvnw
RUN tr -d '\r' < mvnw > mvnw_unix && mv mvnw_unix mvnw && chmod +x mvnw

# Tải trước dependencies để tận dụng Docker layer caching
RUN ./mvnw dependency:go-offline -B || mvn dependency:go-offline -B

# Copy thư mục src và thực hiện build đóng gói ứng dụng
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime image với JRE 21 Alpine tối ưu dung lượng (< 250MB)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tạo user phi đặc quyền (non-root) để chạy ứng dụng an toàn
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy tệp thực thi .jar từ Stage 1
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
