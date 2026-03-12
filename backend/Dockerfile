FROM maven:3.9.9-amazoncorretto-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM amazoncorretto:17
WORKDIR /app
ENV PORT=3000
COPY --from=build /app/target/backend-1.0.0.jar ./app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
