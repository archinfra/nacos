# Nacos SkillHub 交付包拆分设计

## 结论

交付物分成四类，不再用一个 `.run` 同时承担 binary、Docker、Kubernetes 三种安装方式。

```text
nacos-skillhub-<version>-<arch>-bin.tar.gz          # 二进制包，手工或 systemd 使用
nacos-skillhub-<version>-<arch>-docker.tar.gz       # 纯 Docker 离线镜像包
nacos-skillhub-<version>-<arch>-docker.run          # 单机 Docker 一键安装器
nacos-skillhub-<version>-<arch>-k8s.run             # Kubernetes 一键安装器
```

这样现场部署时不会混淆：

- 单机 Docker：用 `-docker.run` 或 `-docker.tar.gz`。
- Kubernetes：用 `-k8s.run`。
- 只要二进制：用 `-bin.tar.gz`。

## Kubernetes `.run` 使用方式

```bash
chmod +x nacos-skillhub-0.1.0-amd64-k8s.run

./nacos-skillhub-0.1.0-amd64-k8s.run install \
  -n a11 \
  --release-name nacos-skillhub \
  --registry sealos.hub:5000/kube4 \
  --registry-user admin \
  --registry-pass PASSW9RD \
  --storage-class standard \
  --storage-size 10Gi \
  --service-type ClusterIP \
  -y
```

状态查看：

```bash
./nacos-skillhub-0.1.0-amd64-k8s.run status -n a11
kubectl get deploy,po,svc,pvc -n a11 -l app.kubernetes.io/instance=nacos-skillhub
```

卸载：

```bash
./nacos-skillhub-0.1.0-amd64-k8s.run uninstall -n a11 -y
```

默认保留 PVC。确实要删除数据时显式加：

```bash
./nacos-skillhub-0.1.0-amd64-k8s.run uninstall -n a11 --delete-pvc -y
```

## Docker `.run` 使用方式

```bash
chmod +x nacos-skillhub-0.1.0-amd64-docker.run

./nacos-skillhub-0.1.0-amd64-docker.run install \
  --docker-name nacos-skillhub \
  --http-port 8848 \
  --grpc-port 9848 \
  --raft-port 9849 \
  -y
```

如需把镜像转推到内网仓库：

```bash
./nacos-skillhub-0.1.0-amd64-docker.run install \
  --registry sealos.hub:5000/kube4 \
  --registry-user admin \
  --registry-pass PASSW9RD \
  -y
```

## Docker 离线镜像包使用方式

```bash
gzip -dc nacos-skillhub-0.1.0-amd64-docker.tar.gz | docker load

docker run -d \
  --name nacos-skillhub \
  --restart unless-stopped \
  -e MODE=standalone \
  -p 8848:8848 \
  -p 9848:9848 \
  -p 9849:9849 \
  ghcr.io/archinfra/apps_nacos-skillhub:0.1.0-amd64
```

## GitHub Release 输出

`git push origin v0.1.0` 后，`.github/workflows/release.yml` 会为 `amd64` 和 `arm64` 分别输出：

```text
nacos-skillhub-0.1.0-amd64-bin.tar.gz
nacos-skillhub-0.1.0-amd64-bin.tar.gz.sha256
nacos-skillhub-0.1.0-amd64-docker.tar.gz
nacos-skillhub-0.1.0-amd64-docker.tar.gz.sha256
nacos-skillhub-0.1.0-amd64-docker.run
nacos-skillhub-0.1.0-amd64-docker.run.sha256
nacos-skillhub-0.1.0-amd64-k8s.run
nacos-skillhub-0.1.0-amd64-k8s.run.sha256
```

arm64 同理。

## 为什么要拆

旧设计用：

```bash
./nacos-skillhub-0.1.0-amd64.run install --install-mode docker
./nacos-skillhub-0.1.0-amd64.run install -n a11
```

这会导致问题：

1. `-n` 在 Docker/binary 模式里没有真实含义。
2. Kubernetes 需要 `kubectl apply`、namespace、PVC、Service、rollout，和 Docker 容器启动完全不是一套逻辑。
3. 离线 K8s 必须考虑镜像导入、重新 tag、推送内网仓库，不能只在本机 `docker load`。
4. 现场交付文档不好写，验收人员也容易执行错包。

新设计把安装器职责固定下来：

- `-docker.run`：只跑 Docker。
- `-k8s.run`：只跑 Kubernetes。
- `.docker.tar.gz`：只作为镜像离线包。
- `.bin.tar.gz`：只作为二进制交付包。
