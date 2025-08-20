FROM openjdk:22-slim
COPY /build/libs/*SNAPSHOT.jar /detailing-crm-backed.jar

ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005","-Dspring.profiles.active=docker-props", "-jar", "detailing-crm-backed.jar"]
