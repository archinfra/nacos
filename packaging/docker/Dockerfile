FROM eclipse-temurin:17-jre-jammy

ARG NACOS_TARBALL=nacos.tar.gz

LABEL org.opencontainers.image.title="nacos-skillhub" \
      org.opencontainers.image.description="ArchInfra Nacos SkillHub distribution" \
      org.opencontainers.image.source="https://github.com/archinfra/apps_nacos-skillhub"

ENV MODE=standalone \
    JAVA_OPT_EXT="" \
    NACOS_HOME=/home/nacos

COPY ${NACOS_TARBALL} /tmp/nacos.tar.gz

RUN set -eux; \
    mkdir -p /tmp/nacos-extract "${NACOS_HOME}"; \
    tar -xzf /tmp/nacos.tar.gz -C /tmp/nacos-extract; \
    extracted="$(find /tmp/nacos-extract -mindepth 1 -maxdepth 1 -type d | head -n 1)"; \
    if [ -z "${extracted}" ]; then extracted=/tmp/nacos-extract; fi; \
    cp -a "${extracted}"/. "${NACOS_HOME}"/; \
    rm -rf /tmp/nacos.tar.gz /tmp/nacos-extract; \
    chmod +x ${NACOS_HOME}/bin/*.sh || true; \
    mkdir -p ${NACOS_HOME}/logs

WORKDIR ${NACOS_HOME}

EXPOSE 8848 9848 9849

CMD ["sh", "-c", "bin/startup.sh -m ${MODE} && tail -F logs/start.out"]
