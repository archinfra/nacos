# Nacos SkillHub Server 改造说明

本项目只改造 Nacos Server。ADK、AgentKit、业务 Agent 如何接入，不放在本仓库实现。

## 边界

Nacos Server 负责：

- Skill 注册中心。
- Skill 草稿、提交、发布、上线、下线。
- Skill 版本管理。
- Skill 标签、业务标签、元数据、可见范围管理。
- Skill ZIP 上传、批量上传、版本下载。
- Runtime ZIP 下载，支持 `200 application/zip`、`304 Not Modified`、`ETag`、`X-Nacos-Skill-Md5`。
- Registry `.well-known/agent-skills` 兼容接口。
- Frontmatter 兼容解析，把扩展字段沉淀到 metadata。
- resourceVersion / md5 / sha256 / If-Match 并发控制。

Nacos Server 不负责：

- ADK 本地缓存实现。
- ADK SkillSource / SkillService 工厂。
- ADK 执行 Skill。
- Agent 运行时热加载策略。

## 核心接口

管理端：

```text
GET    /v3/admin/ai/skills/list
GET    /v3/admin/ai/skills
GET    /v3/admin/ai/skills/version
POST   /v3/admin/ai/skills/draft
PUT    /v3/admin/ai/skills/draft
DELETE /v3/admin/ai/skills/draft
DELETE /v3/admin/ai/skills
POST   /v3/admin/ai/skills/submit
POST   /v3/admin/ai/skills/publish
POST   /v3/admin/ai/skills/online
POST   /v3/admin/ai/skills/offline
PUT    /v3/admin/ai/skills/labels
PUT    /v3/admin/ai/skills/biz-tags
PUT    /v3/admin/ai/skills/metadata
PUT    /v3/admin/ai/skills/scope
POST   /v3/admin/ai/skills/upload
POST   /v3/admin/ai/skills/upload/batch
GET    /v3/admin/ai/skills/version/download
```

运行时：

```text
GET /v3/client/ai/skills?namespaceId={ns}&name={skillName}&version={v}&label={label}&md5={md5}
```

Registry 兼容：

```text
GET /registry/{namespaceId}/.well-known/agent-skills/index.json
GET /registry/{namespaceId}/.well-known/agent-skills/{name}/SKILL.md
GET /registry/{namespaceId}/.well-known/agent-skills/{name}.zip
GET /registry/{namespaceId}/api/search?q=
```

## 版本解析规则

Runtime 下载接口必须使用统一解析优先级：

```text
version > label > latest
```

其中：

- 指定 `version` 时，按精确版本下载。
- 未指定 `version`，但指定 `label` 时，按 label 解析版本。
- 二者都没有时，默认解析 `latest`。

## ZIP 结构兼容

必须同时支持：

```text
SKILL.md
references/...
assets/...
scripts/...
```

以及：

```text
skill-name/SKILL.md
skill-name/references/...
skill-name/assets/...
skill-name/scripts/...
```

## Frontmatter 扩展字段

Nacos 可以解析并存储：

```yaml
skillSet:
groups:
keywords:
modelName:
modelDescription:
matchHint:
activation:
priority:
```

保存时建议同时写入 `metadata`，方便不同 Agent/SDK 按标准字段读取。

## 当前项目交付方式

- 前端 `console-ui-next` 构建后注入 `console/src/main/resources/static/next`。
- 后端由 Maven 构建 Nacos 发行包。
- Release 产物分为：
  - `*-bin.tar.gz`
  - `*-docker.tar.gz`
  - `*-docker.run`
  - `*-k8s.run`
