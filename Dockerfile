# --- Aşama 1: Build (İnşaat) ---
# Maven yüklü bir imaj kullanıyoruz
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app


COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080


ENV DB_URL="jdbc:h2:file:./data/dashboarddb"
ENV DB_USER="sa"
ENV DB_PASS=""
ENV DISCORD_WEBHOOK_URL=""


VOLUME /app/data

ENTRYPOINT ["java", "-jar", "app.jar"]