FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

COPY build/libs/Nifty-1.0.0.jar app.jar

CMD ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]