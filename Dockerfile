FROM maven:3.5.0-jdk-8-alpine as builder

WORKDIR /root
COPY ./ /root/

# Compilation
RUN mvn package

FROM openjdk:8-jdk-alpine

COPY --from=builder /root/target/*.jar /

RUN mv /*.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
