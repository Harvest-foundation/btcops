FROM g4s8/alpine:jdk-8 as build
MAINTAINER Kirill <g4s8.public@gmail.com>
LABEL Description="BTC operations wallet API"
ARG version="1.0-SNAPSHOT"
WORKDIR /build
COPY pom.xml ./pom.xml
COPY src ./src
COPY LICENSE.txt ./LICENSE.txt
COPY .git .git
RUN mvn versions:set -DnewVersion=${version} && \
  mvn install -Pdocker -Pversioning -B --quiet
FROM g4s8/alpine:jre-8
WORKDIR /app
COPY --from=build /build/target/service.jar /app/service.jar
COPY --from=build /build/target/deps /app/deps
EXPOSE 80
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-cp", "service.jar:deps/*", "wtf.harvest.btcops.BtcOps"]
