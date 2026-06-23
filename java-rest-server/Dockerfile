FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
# oauth.properties requis au build (Google OAuth non utilisé en Docker, Keycloak prend le relais)
RUN printf "google.client.id=placeholder\ngoogle.client.secret=placeholder\ngoogle.callback.url=http://localhost:8080/oauth2callback\n" \
    > src/main/resources/oauth.properties
RUN mvn package -DskipTests -q

FROM tomcat:9.0-jdk11-temurin
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/target/java-rest-server.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
