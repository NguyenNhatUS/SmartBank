# ========================
# Stage 1: Build
# ========================
FROM eclipse-temurin:24-jdk AS builder

WORKDIR /app

# Copy Maven wrapper và pom.xml trước để tận dụng Docker layer cache
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached nếu pom.xml không đổi)
RUN ./mvnw dependency:go-offline -B

# Copy source code và build
COPY src src
RUN ./mvnw package -DskipTests -B

# ========================
# Stage 2: Runtime
# ========================
FROM eclipse-temurin:24-jre AS runtime

WORKDIR /app

# Tạo user non-root để chạy app (security best practice)
RUN groupadd --system smartbank && useradd --system --gid smartbank smartbank

# Copy JAR từ stage build
COPY --from=builder /app/target/SmartBank-*.jar app.jar

# Đổi ownership về user non-root
RUN chown smartbank:smartbank app.jar

USER smartbank

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]