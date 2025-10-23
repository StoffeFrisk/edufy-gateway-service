FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 4505
ENTRYPOINT ["java","-jar","/app/app.jar"]