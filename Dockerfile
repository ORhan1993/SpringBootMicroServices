# --- Aşama 1: Build (Derleme) ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Bağımlılıkları önbelleğe almak için önce sadece pom.xml'i kopyalıyoruz
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kaynak kodları kopyalıyoruz
COPY src ./src

# Projeyi derliyoruz
RUN mvn clean package -DskipTests

# --- Aşama 2: Run (Çalıştırma) ---
# HATA DÜZELTİLDİ: 'openjdk' yerine 'eclipse-temurin' kullanıyoruz
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Derlenen jar dosyasını kopyalıyoruz
COPY --from=build /app/target/paymentService-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]