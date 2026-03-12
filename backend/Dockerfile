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
RUN yum install -y curl \
  && yum clean all \
  && useradd --system --create-home --uid 10001 appuser \
  && chown -R appuser:appuser /app
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD curl --fail --silent http://127.0.0.1:${PORT}/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
