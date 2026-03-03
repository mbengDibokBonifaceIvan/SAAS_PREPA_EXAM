# Étape 1 : Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache des dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compilation
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2 : Run
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Sécurité : Création d'un utilisateur non-root
RUN useradd -ms /bin/bash springuser
USER springuser

# On est spécifique sur le JAR source pour éviter les surprises
COPY --from=build /app/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# Configuration JVM (ZGC est un excellent choix pour une Gateway !)
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "app.jar"]