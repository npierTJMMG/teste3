FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY pom.xml ./

RUN apk add --no-cache maven
RUN mvn dependency:go-offline

COPY . .

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/ism-1.0.0.jar"]
