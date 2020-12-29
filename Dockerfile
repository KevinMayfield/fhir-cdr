FROM openjdk:11-slim
VOLUME /tmp

ENV JAVA_OPTS="-Xms128m -Xmx4096m"

ADD target/fhir-cdr.jar fhir-cdr.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/fhir-cdr.jar"]


