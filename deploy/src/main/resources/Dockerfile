FROM openjdk:13-ea-19-alpine

# let's get bash, nss (see  https://github.com/docker-library/openjdk/issues/289)
RUN apk update && apk upgrade && apk add --no-cache nss bash openssl

MAINTAINER "Aaron Pritzlaff"
EXPOSE 80
EXPOSE 9010

RUN mkdir -p /app/data && \
    mkdir -p /app/logs && \
    mkdir -p /app/jfr && \
    mkdir -p /app/scripts && \
    mkdir -p /app/lib && \
    mkdir -p /app/heapdump/

ENV DATA_DIR /app/data/
ENV JMX_PORT 9010
VOLUME /app/data /app/config /app/logs /app/heapdump /app/web

ADD logback.xml /app/config/logback.xml
ADD application.conf /app/config/application.conf
ADD app-docker.conf /app/config/app-docker.conf
ADD app.jar /app/lib/app.jar
ADD boot.sh /app/boot.sh

# https://serverfault.com/questions/772227/chmod-not-working-correctly-in-docker
# RUN chmod +x /app/scripts/*.sh

RUN chmod 700 /app/boot.sh
WORKDIR /app

ENTRYPOINT ["/app/boot.sh"]
