FROM maven:3.9.9-amazoncorretto-17 AS base
WORKDIR /app
COPY pom.xml ./
COPY src ./src

FROM base AS test
RUN mvn -q test

FROM base AS build
RUN mvn -q -DskipTests package

FROM amazoncorretto:17
WORKDIR /app
ENV PORT=8080
COPY --from=build /app/target/backend-1.0.0.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
