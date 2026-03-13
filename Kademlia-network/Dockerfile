
FROM maven:3.9-eclipse-temurin-23-alpine AS builder
LABEL author=hugo
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests


FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
RUN addgroup -S p2pgroup && adduser -S p2puser -G p2pgroup

COPY --from=builder --chown=p2puser:p2pgroup /build/target/Kademlia-network-1.0-SNAPSHOT.jar /app/p2p-node.jar

RUN chown -R p2puser:p2pgroup /app

RUN rm -rf /var/cache/apk/*
USER p2puser
EXPOSE 8000-8010
EXPOSE 9000-9010

CMD ["java", "-version"]