# Nacos SkillHub 一体化构建说明

本仓库是 Nacos SkillHub 的一体化源码与交付仓库，包含：

```text
console-ui-next/                         # 前端源码
console/                                 # Java console 后端
server/config/naming/auth/...            # Nacos 后端模块
skills/                                  # SkillHub 相关模块
packaging/docker/Dockerfile              # Docker 镜像构建
packaging/k8s/                           # Kubernetes manifest 模板
packaging/run-docker/                    # 单机 Docker .run 安装器
packaging/run-k8s/                       # Kubernetes .run 安装器
.github/workflows/                       # CI/CD
```

## 构建依赖

源码构建依赖：

```text
JDK 17+
Maven 3.6.3+
Node.js 22+
npm/pnpm，按前端 lock 文件选择
```

交付构建依赖：

```text
Docker
docker buildx
QEMU
tar/gzip/sha256sum
kubectl，只有验证 k8s 安装器时需要
```

## 前端与后端关系

前端不是单独部署。默认链路是：

```text
console-ui-next build
  -> dist/
  -> copy 到 console/src/main/resources/static/next
  -> Maven 编译 Java console
  -> distribution 生成 nacos-server tar.gz
```

之前的错误：

```text
cp: cannot create directory '../console/src/main/resources/static/next': No such file or directory
```

已经通过 `console-ui-next/build/copyFile.cjs` 修复。脚本会自动创建目标目录。

## 本地构建

```bash
bash scripts/build-frontend.sh
bash scripts/build-backend.sh --skip-frontend
```

或：

```bash
bash scripts/build-all.sh
```

查找最终 Nacos 发行包：

```bash
bash scripts/find-nacos-dist.sh
```

## 静态校验

```bash
bash scripts/verify-static.sh
```

## Release 交付物

`git push origin v0.1.0` 后，`.github/workflows/release.yml` 会构建 `amd64` 和 `arm64` 两套交付物：

```text
nacos-skillhub-0.1.0-amd64-bin.tar.gz
nacos-skillhub-0.1.0-amd64-docker.tar.gz
nacos-skillhub-0.1.0-amd64-docker.run
nacos-skillhub-0.1.0-amd64-k8s.run
```

arm64 同理。

## 交付物用途

| 交付物 | 用途 |
|---|---|
| `-bin.tar.gz` | 二进制软件包，可手工部署或二次封装 systemd |
| `-docker.tar.gz` | Docker 离线镜像包，只做镜像导入 |
| `-docker.run` | 单机 Docker 一键安装器 |
| `-k8s.run` | Kubernetes 一键安装器，支持 namespace、PVC、Service、Deployment |

## Kubernetes 安装

```bash
./nacos-skillhub-0.1.0-amd64-k8s.run install \
  -n a11 \
  --release-name nacos-skillhub \
  --registry sealos.hub:5000/kube4 \
  --registry-user admin \
  --registry-pass PASSW9RD \
  --storage-size 10Gi \
  --service-type ClusterIP \
  -y
```

说明：

- `-n/--namespace` 在 `-k8s.run` 中是真正的 Kubernetes namespace。
- `--registry` 建议必填，安装器会 `docker load -> docker tag -> docker push`，然后将 manifest 中的镜像替换为内网仓库地址。
- 默认保留 PVC，卸载时不会误删数据。

状态：

```bash
./nacos-skillhub-0.1.0-amd64-k8s.run status -n a11
```

卸载：

```bash
./nacos-skillhub-0.1.0-amd64-k8s.run uninstall -n a11 -y
```

删除 PVC：

```bash
./nacos-skillhub-0.1.0-amd64-k8s.run uninstall -n a11 --delete-pvc -y
```

## Docker 安装

```bash
./nacos-skillhub-0.1.0-amd64-docker.run install \
  --docker-name nacos-skillhub \
  --http-port 8848 \
  --grpc-port 9848 \
  --raft-port 9849 \
  -y
```

状态：

```bash
./nacos-skillhub-0.1.0-amd64-docker.run status
```

卸载：

```bash
./nacos-skillhub-0.1.0-amd64-docker.run uninstall -y
```

## Auth 参数

Docker 和 K8s 安装器都支持：

```bash
--auth-enable true
--auth-token '<at-least-32-chars-token>'
--identity-key serverIdentity
--identity-value security
```

生产环境建议显式启用认证并设置强随机 token。

## 设计原则

不要再使用一个 `.run` 加 `--install-mode docker|k8s|binary` 混合安装。现在按职责拆分：

```text
-docker.run 只服务 Docker
-k8s.run    只服务 Kubernetes
-docker.tar.gz 只服务离线镜像导入
-bin.tar.gz 只服务二进制交付
```
