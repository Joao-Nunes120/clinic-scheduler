FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src/ src/

RUN ./mvnw -B -Dmaven.test.skip=true package


FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

ENV TZ=Europe/Lisbon

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Europe/Lisbon", "-jar", "app.jar"]