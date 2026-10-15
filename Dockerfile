# Stage 1: Build source code với Maven và JDK 21
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Biên dịch ứng dụng
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image với JRE 21 tối ưu hóa dung lượng
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tạo non-root user cho môi trường production
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy file jar từ build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
