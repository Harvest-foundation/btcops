FROM g4s8/alpine:jdk-8 as build
MAINTAINER Kirill <g4s8.public@gmail.com>
LABEL Description="Harvest BTC operations bot"
ARG version="1.0-SNAPSHOT"
WORKDIR /build
COPY pom.xml ./pom.xml
COPY src ./src
#COPY LICENSE.txt ./LICENSE.txt
#COPY .git .git
RUN mvn versions:set -DnewVersion=${version} && \
  mvn install -Pqulice -Pdocker -Pversioning -B --quiet
FROM g4s8/alpine:jre-8
WORKDIR /app
COPY --from=build /build/target/core.jar /app/service.jar
COPY --from=build /build/target/deps /app/deps
CMD ["-Dfile.encoding=UTF-8",  "-cp", "service.jar:deps/*", "wtf.harvest.btcops.Bot", "--port=80"]
EXPOSE 80
