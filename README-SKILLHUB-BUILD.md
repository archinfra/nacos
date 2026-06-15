# Nacos SkillHub 一体化构建说明

这个仓库是一个完整的一体化 Nacos 二开交付工程，不再只是拿官方 `nacos/nacos-server` 镜像做离线安装包。

它的构建链路是：

```text
console-ui-next 前端源码
  -> npm run build
  -> dist/
  -> 注入 console/src/main/resources/static/next
  -> Maven 构建 Nacos 后端与 distribution
  -> 生成 bin 包 / Docker 离线镜像包 / .run 一键安装包
```

## 目录关系

```text
console-ui-next/                               # 新控制台前端
console/                                      # Nacos 控制台 Java 后端
console/src/main/resources/static/next/       # 前端构建产物注入位置，构建时自动生成
packaging/docker/Dockerfile                   # 用 distribution tar.gz 构建 Docker 镜像
packaging/offline-run/                        # .run 自解压安装器
scripts/build-frontend.sh                     # 构建前端并注入后端静态目录
scripts/build-backend.sh                      # 构建后端 distribution
scripts/build-all.sh                          # 前端 + 后端完整构建
.github/workflows/frontend.yml                # 前端 CI
.github/workflows/backend.yml                 # 后端 CI
.github/workflows/docker.yml                  # 手动 Docker 交付构建
.github/workflows/release.yml                 # tag 后构建 bin/docker/run 多架构 Release
```

## 修复的前端问题

原来的前端 build 脚本是：

```bash
rm -rf ../console/src/main/resources/static/next && cp -r dist ../console/src/main/resources/static/next
```

在目标父目录不存在时会失败：

```text
cp: cannot create directory '../console/src/main/resources/static/next': No such file or directory
```

现在改为：

```json
"build": "tsc -b && vite build && node build/copyFile.cjs"
```

`build/copyFile.cjs` 会自动：

1. 检查 `dist/` 是否存在；
2. 创建 `console/src/main/resources/static/` 父目录；
3. 删除旧的 `next/`；
4. 递归复制 `dist/` 到 `next/`；
5. 校验 `index.html` 是否存在。

## 本地构建

```bash
bash scripts/build-frontend.sh
bash scripts/build-backend.sh --skip-frontend
```

或者：

```bash
bash scripts/build-all.sh
```

## tag 发布

```bash
git tag v0.1.0
git push origin v0.1.0
```

Release 会按 `amd64` 和 `arm64` 构建：

```text
nacos-skillhub-0.1.0-amd64-bin.tar.gz
nacos-skillhub-0.1.0-amd64-docker.tar.gz
nacos-skillhub-0.1.0-amd64.run

nacos-skillhub-0.1.0-arm64-bin.tar.gz
nacos-skillhub-0.1.0-arm64-docker.tar.gz
nacos-skillhub-0.1.0-arm64.run
```

## .run 安装示例

二进制 systemd 安装：

```bash
chmod +x nacos-skillhub-0.1.0-amd64.run
./nacos-skillhub-0.1.0-amd64.run install --install-mode binary -y
```

Docker 安装：

```bash
./nacos-skillhub-0.1.0-amd64.run install --install-mode docker -y
```

推送到内网仓库后 Docker 安装：

```bash
./nacos-skillhub-0.1.0-amd64.run install \
  --install-mode docker \
  --registry sealos.hub:5000/kube4 \
  --registry-user admin \
  --registry-pass 'PASSW9RD' \
  -y
```
