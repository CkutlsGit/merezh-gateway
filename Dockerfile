FROM eclipse-temurin:21-alpine

WORKDIR /app

RUN addgroup -S gateway && adduser -S gateway -G gateway

COPY target/gateway-merezh-0.0.1-SNAPSHOT.jar /app/gateway-merezh.jar

RUN chown -R gateway:gateway /app

USER gateway

ENTRYPOINT ["java", "-jar", "gateway-merezh.jar"]