FROM openjdk:8-jdk-alpine
EXPOSE 8080

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["java", "-jar", "ping-0.1.0-SNAPSHOT.jar"]

RUN apk add --update ca-certificates tini && \
    adduser -u 10001 -S -g /opt/app app && \
    rm -rf /var/cache/apk/* /tmp/*

USER app
WORKDIR /opt/app

COPY build/libs/*.jar /opt/app
