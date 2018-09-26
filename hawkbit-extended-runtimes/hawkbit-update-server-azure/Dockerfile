FROM mcr.microsoft.com/java/jre-headless:8u181-zulu-alpine

MAINTAINER Kai Zimmermann <kai.zimmermann@microsoft.com>

ARG JAR_FILE=hawkbit-update-server-azure-0.3.0-SNAPSHOT.jar

ENV HAWKBIT_HOME=/opt/hawkbit

EXPOSE 8080

VOLUME "$HAWKBIT_HOME/data"

WORKDIR $HAWKBIT_HOME
ENTRYPOINT ["java","-jar","hawkbit-update-server.jar","-Xmx768m -Xmx768m -XX:MaxMetaspaceSize=250m -XX:MetaspaceSize=250m -Xss300K -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+HeapDumpOnOutOfMemoryError"]

# Add the service itself
ADD target/$JAR_FILE hawkbit-update-server.jar
