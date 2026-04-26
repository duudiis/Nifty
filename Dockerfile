FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

COPY . .

RUN ./gradlew jar --no-daemon

FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/Nifty-1.0.0.jar app.jar

CMD ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]