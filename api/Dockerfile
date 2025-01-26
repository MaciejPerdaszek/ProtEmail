FROM eclipse-temurin:22-jdk
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline
COPY . .
RUN ./mvnw clean package -DskipTests
RUN mv target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]