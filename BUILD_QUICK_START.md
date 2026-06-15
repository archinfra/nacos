# Nacos SkillHub 快速构建与交付

## 本地构建源码

```bash
bash scripts/build-frontend.sh
bash scripts/build-backend.sh --skip-frontend
```

或一次性构建：

```bash
bash scripts/build-all.sh
```

前端会被自动复制到：

```text
console/src/main/resources/static/next
```

## 静态校验

```bash
bash scripts/verify-static.sh
```

## Tag 发布

```bash
git tag v0.1.0
git push origin v0.1.0
```

GitHub Release 会输出四类交付物：

```text
-bin.tar.gz       二进制包
-docker.tar.gz    Docker 离线镜像包
-docker.run       单机 Docker 一键安装器
-k8s.run          Kubernetes 一键安装器
```

## Kubernetes 安装

```bash
./nacos-skillhub-0.1.0-amd64-k8s.run install \
  -n a11 \
  --registry sealos.hub:5000/kube4 \
  --registry-user admin \
  --registry-pass PASSW9RD \
  -y
```

## Docker 安装

```bash
./nacos-skillhub-0.1.0-amd64-docker.run install -y
```

更多说明见：

```text
RUN_PACKAGE_DESIGN.md
README-SKILLHUB-BUILD.md
```
