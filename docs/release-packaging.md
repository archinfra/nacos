# Nacos Backend tag release and offline `.run` packaging

This repository supports tag-based release packaging for the backend fork.

## Release trigger

Push a Git tag that starts with `v`:

```bash
git tag v0.1.0
git push origin v0.1.0
```

GitHub Actions will run `.github/workflows/backend-release.yml` and build release assets for both `amd64` and `arm64`.

## Produced assets

For each architecture, the workflow produces:

```text
nacos-backend-<version>-<arch>-bin.tar.gz
nacos-backend-<version>-<arch>-bin.tar.gz.sha256
nacos-backend-<version>-<arch>-docker.tar.gz
nacos-backend-<version>-<arch>-docker.tar.gz.sha256
nacos-backend-<version>-<arch>.run
nacos-backend-<version>-<arch>.run.sha256
```

On tag builds, all assets are attached to the GitHub Release. A combined `SHA256SUMS` file is also generated.

## Offline `.run` installer usage

Binary/systemd mode:

```bash
chmod +x nacos-backend-0.1.0-amd64.run
sudo ./nacos-backend-0.1.0-amd64.run install -y
```

Docker mode:

```bash
./nacos-backend-0.1.0-amd64.run install --install-mode docker -y
```

Docker mode with internal registry retag and push:

```bash
./nacos-backend-0.1.0-amd64.run install \
  --install-mode docker \
  --registry sealos.hub:5000/kube4 \
  --registry-user admin \
  --registry-pass PASSW9RD \
  -y
```

Unpack only:

```bash
./nacos-backend-0.1.0-amd64.run unpack --install-dir /tmp/nacos-backend-payload -y
```

Status:

```bash
./nacos-backend-0.1.0-amd64.run status
```

Uninstall service/container, keeping installed files:

```bash
sudo ./nacos-backend-0.1.0-amd64.run uninstall -y
```

## Installer options

```text
--install-mode <binary|docker|unpack>  Install mode. Default: binary.
--nacos-mode <standalone|cluster>      Nacos startup mode. Default: standalone.
--install-dir <dir>                    Binary install directory. Default: /opt/nacos-backend.
--service-name <name>                  systemd service name. Default: nacos-backend.
--docker-name <name>                   Docker container name. Default: nacos-backend.
--registry <repo-prefix>               Retag/push loaded image to internal registry.
--registry-user <user>                 Registry username.
--registry-pass <pass>                 Registry password.
--skip-image-prepare                   Skip docker load/tag/push.
--http-port <port>                     Host HTTP port. Default: 8848.
--grpc-port <port>                     Host gRPC port. Default: 9848.
--raft-port <port>                     Host raft port. Default: 9849.
-y, --yes                              Skip confirmation.
```

## Local build after Maven package

If you already have a backend distribution and docker image tar, you can build `.run` manually:

```bash
bash packaging/offline-run/build.sh \
  --arch amd64 \
  --version 0.1.0 \
  --binary distribution/target/nacos-server-*.tar.gz \
  --docker-image dist/nacos-backend-0.1.0-amd64-docker.tar.gz \
  --image-ref ghcr.io/archinfra/nacos-backend:0.1.0-amd64 \
  --out-dir dist
```

## Design notes

The `.run` file is a self-extracting shell installer. `packaging/offline-run/build.sh` creates `payload.tar.gz` and appends it after `packaging/offline-run/install.sh` using the marker line:

```text
__PAYLOAD_BELOW__
```

The installer extracts the embedded payload by byte offset instead of line count, which is safer for binary payload boundaries.
