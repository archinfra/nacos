# 快速开始

## 1. 前端构建并注入后端

```bash
bash scripts/build-frontend.sh
```

这个命令会把 `console-ui-next/dist` 复制到：

```text
console/src/main/resources/static/next
```

## 2. 后端构建

```bash
bash scripts/build-backend.sh --skip-frontend
```

## 3. 完整构建

```bash
bash scripts/build-all.sh
```

## 4. 发布

```bash
git tag v0.1.0
git push origin v0.1.0
```

`.github/workflows/release.yml` 会自动构建 `amd64/arm64` 的 `bin`、`docker.tar.gz`、`.run` 三类交付物。
