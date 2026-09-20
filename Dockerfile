FROM eclipse-temurin:8-jdk

WORKDIR /app

COPY . .

RUN javac -cp "mysql-connector-j-8.0.33.jar" ProductServer.java

CMD ["java", "-cp", ".:mysql-connector-j-8.0.33.jar", "ProductServer"]
