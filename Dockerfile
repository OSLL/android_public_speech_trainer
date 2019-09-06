FROM ubuntu:18.04

ENV ANDROID_HOME /opt/android-sdk-linux

RUN apt-get update -qq
RUN dpkg --add-architecture i386
RUN apt-get update -qq
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-8-jdk libc6:i386 libstdc++6:i386 libgcc1:i386 libncurses5:i386 libz1:i386 wget unzip npm gem

RUN cd /opt \
    && wget -q https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip -O android-sdk-tools.zip \
    && unzip -q android-sdk-tools.zip -d ${ANDROID_HOME} \
    && rm android-sdk-tools.zip

ENV PATH ${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/platform-tools
RUN yes | sdkmanager --licenses

RUN sdkmanager "emulator" "tools" "platform-tools"

RUN yes | sdkmanager \
    "platforms;android-28" \
    "platforms;android-27" \
    "build-tools;28.0.3" \
    "build-tools;27.0.3"

RUN apt-get update \
 && apt-get -y install gradle \
 && gradle -v

ADD . /app
WORKDIR /app
ENTRYPOINT "scripts/docker_entrypoint.sh" && /bin/bash
