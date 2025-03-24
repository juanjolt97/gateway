#First stage: complete build environment

FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /Gateway
COPY pom.xml .
COPY src ./src
ENV SPRING_PROFILES_ACTIVE=prod
RUN mvn clean package

#Second Stage: Last image

FROM openjdk:17-jdk-slim
WORKDIR /Gateway
COPY --from=build /Gateway/target/gateway-1.0.1.jar .
ENV SPRING_PROFILES_ACTIVE=prod
CMD [ "java" , "-jar" , "gateway-1.0.1.jar" ]