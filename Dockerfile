# ── Etapa 1: compilación ────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Descarga dependencias antes de copiar el código (cache eficiente)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Etapa 2: imagen de ejecución ────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S zymos && adduser -S zymos -G zymos

COPY --from=build /app/target/alera-*.jar app.jar
RUN chown zymos:zymos app.jar

USER zymos

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
