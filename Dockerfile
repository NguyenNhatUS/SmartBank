FROM eclipse-temurin:24-jdk AS builder

WORKDIR /app


COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN ./mvnw dependency:go-offline -B


COPY src src
RUN ./mvnw package -DskipTests -B



FROM eclipse-temurin:24-jre AS runtime

WORKDIR /app

RUN groupadd --system smartbank && useradd --system --gid smartbank smartbank

COPY --from=builder /app/target/SmartBank-*.jar app.jar

RUN chown smartbank:smartbank app.jar

USER smartbank

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]