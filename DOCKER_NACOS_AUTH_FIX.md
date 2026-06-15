# Nacos auth token injection fix

Nacos source distribution `bin/startup.sh` validates `conf/application.properties` before starting Java. In this custom image, Kubernetes/Docker environment variables such as `NACOS_AUTH_TOKEN` must be written back into `application.properties` before `startup.sh` runs.

This project now ships `packaging/docker/entrypoint.sh`, which maps:

- `NACOS_AUTH_TOKEN` -> `nacos.core.auth.plugin.nacos.token.secret.key`
- `NACOS_AUTH_IDENTITY_KEY` -> `nacos.core.auth.server.identity.key`
- `NACOS_AUTH_IDENTITY_VALUE` -> `nacos.core.auth.server.identity.value`
- `NACOS_AUTH_ENABLE` -> `nacos.core.auth.enabled`

If `NACOS_AUTH_TOKEN` is missing and the config file is empty, the entrypoint generates a valid Base64 token so the container can start. Production deployments should still inject a fixed Kubernetes Secret.
