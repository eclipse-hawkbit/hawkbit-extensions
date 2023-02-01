FROM ubuntu:22.04 AS build

ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y openjdk-11-jdk-headless maven git

WORKDIR /src
RUN git clone https://github.com/eclipse/hawkbit
WORKDIR /src/hawkbit
RUN mvn --quiet --batch-mode --threads 1C dependency:go-offline dependency:resolve-plugins
RUN mvn --quiet --batch-mode --threads 1C --define=skipTests package
RUN mvn --quiet --batch-mode --threads 1C --define=skipTests --offline install

WORKDIR /src/hawkbit-extensions
COPY . .
RUN mvn --quiet --batch-mode --threads 1C dependency:go-offline dependency:resolve-plugins
RUN mvn --quiet --batch-mode --threads 1C package
RUN mvn --quiet --batch-mode --threads 1C --offline install
