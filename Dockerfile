FROM openjdk
COPY target/sdp-1.0.jar sdp.jar
ENTRYPOINT ["java","-jar","/sdp.jar","--spring.config.location=file:properties.yaml"]