FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends xvfb libgl1-mesa-dri libgl1 libglu1-mesa libxrandr2 libxcursor1 libxi6 libxxf86vm1 libxinerama1 libasound2 python3 python3-pip ca-certificates \
    && pip3 install --no-cache-dir portablemc==4.4.0 \
    && rm -rf /var/lib/apt/lists/*

ARG UID=1000
RUN useradd -m -u "$UID" mc
USER mc
WORKDIR /home/mc

COPY --chown=mc:mc entrypoint.sh /home/mc/entrypoint.sh
COPY --chown=mc:mc options.txt /home/mc/options.txt
RUN chmod +x /home/mc/entrypoint.sh

ENV MC_VERSION=1.21.5 \
    MC_LOGIN= \
    MC_SERVER=hugosmp.net \
    JAVA_OPTS="-Xmx2G -Xms512M -XX:+UseSerialGC"

ENTRYPOINT ["/home/mc/entrypoint.sh"]
