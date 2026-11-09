# Stage 1: Build source code với Maven JDK 21 Alpine
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
ENV MAVEN_CONFIG=""

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

# Cài đặt curl và tzdata để cấu hình múi giờ UTC+7 (Asia/Ho_Chi_Minh)
RUN apk --no-cache add curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone
ENV TZ=Asia/Ho_Chi_Minh

# Tạo user phi đặc quyền (non-root) để chạy ứng dụng an toàn
RUN addgroup -S app && adduser -S app -G app
USER app:app

# Copy tệp thực thi .jar từ Stage 1 với quyền thuộc về user app
COPY --from=build --chown=app:app /app/target/*.jar app.jar

EXPOSE 8080

# Khai báo HEALTHCHECK giám sát trạng thái ứng dụng qua Spring Boot Actuator
HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=30s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
