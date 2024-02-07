FROM openjdk:8-jdk-alpine
WORKDIR /usr/local/billing/
MAINTAINER iqbal.idtrust
COPY bank /usr/local/billing/bank
COPY billing.jar /usr/local/billing/billing.jar
COPY spring.keystore /usr/local/billing/spring.keystore
ENTRYPOINT ["java","-jar","/usr/local/billing/billing.jar"]
