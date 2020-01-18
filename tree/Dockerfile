FROM openjdk:8-jdk-alpine

COPY build/libs/*.jar app.jar

EXPOSE $APP_PORT

CMD java $JAVA_OPTS -jar app.jar