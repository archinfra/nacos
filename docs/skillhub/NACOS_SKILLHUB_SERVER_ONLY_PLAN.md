# Nacos SkillHub Server Only 开发计划

本文档只描述 Nacos Server 侧需要做的事情，不包含 ADK 侧实现。

## P0：运行时分发闭环

目标：让任何外部 Agent/SDK 可以通过 Runtime API 稳定下载 Skill ZIP。

### 必须完成

- `GET /v3/client/ai/skills` 支持 `namespaceId`、`name`、`version`、`label`、`md5`。
- 返回 `200 application/zip`。
- 命中 `md5` 时返回 `304 Not Modified`。
- 返回头：
  - `ETag: <md5>`
  - `X-Nacos-Skill-Md5: <md5>`
  - `X-Nacos-Skill-Resolved-Version: <version>`
  - `X-Nacos-Skill-Sha256: <sha256>`，建议新增。
- 版本解析优先级固定为 `version > label > latest`。
- 只允许下载 `enable=true`、`online=true` 的 Skill。

### 验收

```bash
BASE_URL=http://127.0.0.1:8848 \
NAMESPACE_ID=public \
SKILL_NAME=demo-skill \
bash scripts/skillhub-smoke-test.sh
```

## P1：管理端能力闭环

目标：控制台和外部管理系统可以完整维护 Skill。

### 必须完成

- 草稿创建、更新、删除。
- 提交审核。
- 发布版本。
- 上线/下线。
- 删除 Skill。
- 更新 labels、bizTags、metadata、scope。
- ZIP 上传、批量上传、版本下载。
- 列表、详情、版本详情。

### 关键字段

```text
namespaceId
skillName
version
editingVersion
reviewingVersion
publishedVersion
online
open
enable
scope
labels
bizTags
metadata
resourceVersion
md5
sha256
downloadCount
createTime
updateTime
```

## P2：Registry 兼容

目标：对外提供静态风格 Registry 接口，方便非 Nacos SDK 使用。

### 必须完成

```text
/registry/{namespaceId}/.well-known/agent-skills/index.json
/registry/{namespaceId}/.well-known/agent-skills/{name}/SKILL.md
/registry/{namespaceId}/.well-known/agent-skills/{name}.zip
/registry/{namespaceId}/api/search?q=
```

### 过滤规则

Registry 只暴露：

```text
scope = PUBLIC
enable = true
online = true
```

## P3：并发控制与治理

目标：避免多个控制台、Agent 或自动化流程同时修改同一个 Skill 时互相覆盖。

### 建议新增

- `resourceVersion`。
- `sha256`。
- `If-Match` 支持。
- `412 Precondition Failed` 错误码。
- 更新接口支持 `expectedResourceVersion`。

## Controller 建议

Nacos Server 侧建议保持三类 Controller：

```text
SkillAdminController     /v3/admin/ai/skills
SkillClientController    /v3/client/ai/skills
SkillsRegistryController /registry/{namespaceId}
```

Console Controller 可以继续走 Proxy/Handler 转发，不直接承载核心业务逻辑。

## Service 建议

```text
SkillOperationService       管理端写操作和查询
SkillClientOperationService Runtime 解析和下载
SkillIndexManifestService   Registry index 生成
SkillDownloadCountManager   下载计数
SkillZipParser              ZIP 解析和校验
SkillMetadataUtils          frontmatter metadata 兼容
SkillContentDigestUtils     md5/sha256 计算
```

## 不进入 Nacos 的内容

以下内容交给外部 ADK/Agent 系统实现：

- 本地 Skill 缓存目录。
- Agent 执行 Skill 的方式。
- SkillSource / SkillService 工厂。
- 运行时热加载。
- Agent 上下文注入。
