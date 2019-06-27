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

# Platform tools
RUN sdkmanager "emulator" "tools" "platform-tools"

RUN yes | sdkmanager \
    "platforms;android-28" \
    "platforms;android-27" \
#    "platforms;android-26" \
#   "platforms;android-25" \
#    "platforms;android-24" \
#    "platforms;android-23" \
#    "platforms;android-22" \
#    "platforms;android-21" \
#    "platforms;android-19" \
#    "platforms;android-17" \
#    "platforms;android-15" \
    "build-tools;28.0.3" \
#    "build-tools;28.0.2" \
#    "build-tools;28.0.1" \
#    "build-tools;28.0.0" \
    "build-tools;27.0.3"
#    "build-tools;27.0.2" \
#    "build-tools;27.0.1" \
#    "build-tools;27.0.0" \
#    "build-tools;26.0.2" \
#    "build-tools;26.0.1" \
#    "build-tools;25.0.3" \
#    "build-tools;24.0.3" \
#    "build-tools;23.0.3" \
#    "build-tools;22.0.1" \
#    "build-tools;21.1.2" \
#    "build-tools;19.1.0" \
#    "build-tools;17.0.0" \
#    "system-images;android-28;google_apis;x86" \
#    "system-images;android-26;google_apis;x86" \
#    "system-images;android-25;google_apis;armeabi-v7a" \
#    "system-images;android-24;default;armeabi-v7a" \
#    "system-images;android-22;default;armeabi-v7a" \
#    "system-images;android-19;default;armeabi-v7a" \
#    "extras;android;m2repository" \
#    "extras;google;m2repository" \
#    "extras;google;google_play_services" \
#    "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.2" \
#    "extras;m2repository;com;android;support;constraint;constraint-layout;1.0.1" \
#    "add-ons;addon-google_apis-google-23" \
#    "add-ons;addon-google_apis-google-22" \
#    "add-ons;addon-google_apis-google-21"

RUN apt-get update \
 && apt-get -y install gradle \
 && gradle -v

# RUN apt-get purge maven maven2 \
#  && apt-get update \
#  && apt-get -y install maven \
#  && mvn --version

# RUN npm install -g ionic cordova

# RUN gem install fastlane --no-document \
#  && fastlane --version

# RUN echo "deb https://packages.cloud.google.com/apt cloud-sdk-`lsb_release -c -s` main" | sudo tee -a /etc/apt/sources.list.d/google-cloud-sdk.list
# RUN curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
# RUN sudo apt-get update -qq \
#  && sudo apt-get install -y -qq google-cloud-sdk

# ENV GCLOUD_SDK_CONFIG /usr/lib/google-cloud-sdk/lib/googlecloudsdk/core/config.json

# gcloud config doesn't update config.json. See the official Dockerfile for details:
#  https://github.com/GoogleCloudPlatform/cloud-sdk-docker/blob/master/Dockerfile
# RUN /usr/bin/gcloud config set --installation component_manager/disable_update_check true \
#  && sed -i -- 's/\"disable_updater\": false/\"disable_updater\": true/g' $GCLOUD_SDK_CONFIG \
#  && /usr/bin/gcloud config set --installation core/disable_usage_reporting true \
#  && sed -i -- 's/\"disable_usage_reporting\": false/\"disable_usage_reporting\": true/g' $GCLOUD_SDK_CONFIG
