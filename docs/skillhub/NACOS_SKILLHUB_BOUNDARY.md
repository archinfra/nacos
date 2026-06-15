# Nacos SkillHub 边界设计

## 1. 总体边界

本仓库把 SkillHub 拆成两个边界：

```text
Nacos Server
  负责：注册中心、元数据、版本、发布、上下线、ZIP 存储/分发、Registry 兼容。

外部 ADK Skill 模块
  负责：从 Nacos 拉取 Skill、解析 SKILL.md、缓存 ZIP、执行时加载指令和资源、将本地编辑保存回 Nacos。
```

原则：

1. Nacos 不执行 Skill，只管理和分发 Skill。
2. ADK 不直接管理 Nacos 存储表，只走 HTTP API。
3. Runtime 下载接口只暴露已发布、已上线、enable=true、权限可见的 Skill。
4. 管理接口允许 draft/review/publish/online/offline 全生命周期。
5. 版本解析优先级固定为 `version > label > latest`。
6. ZIP 内容支持两种根结构：`SKILL.md` 位于根目录，或者 `skill-name/SKILL.md` 位于一级目录。

## 2. Nacos Server 需要负责的能力

### 2.1 管理 API

管理 API 位于：

```text
/v3/admin/ai/skills
```

用于后台管理、平台管理、Agent 自动沉淀 Skill。

必须支持：

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

### 2.2 Runtime 下载 API

Runtime 下载 API 位于：

```text
/v3/client/ai/skills
```

这是 ADK 运行时最关键的接口。

```http
GET /v3/client/ai/skills?namespaceId={ns}&name={skillName}&version={v}&label={label}&md5={md5}
```

必须支持：

```text
200 application/zip
304 Not Modified
ETag: <md5>
X-Nacos-Skill-Md5: <md5>
X-Nacos-Skill-Resolved-Version: v1
```

### 2.3 Registry 兼容接口

用于 skills CLI 或生态工具发现 Skill。

```text
/registry/{namespaceId}/.well-known/agent-skills/index.json
/registry/{namespaceId}/.well-known/agent-skills/{name}/SKILL.md
/registry/{namespaceId}/.well-known/agent-skills/{name}.zip
/registry/{namespaceId}/api/search?q=
```

只暴露：

```text
scope = PUBLIC
status = published/online
online = true
enable = true
```

## 3. 外部 ADK Skill 模块需要负责的能力

### 3.1 配置扩展

```yaml
skills:
  enabled: true
  source: nacos # filesystem | nacos
  preload: frontmatter
  nacos:
    endpoint: http://36.138.61.152:32492
    context_path: /nacos
    namespace_id: public
    username: nacos
    password_env: NACOS_PASSWORD
    runtime_label: latest
    cache_root: ./.adk/cache/nacos-skills
```

### 3.2 Nacos Client

ADK 侧需要封装：

```text
Login()
ListSkills()
GetSkillMeta()
GetSkillVersion()
CreateDraft()
UpdateDraft()
Submit()
Publish()
Online()
Offline()
UploadZip()
DownloadRuntimeZip()
DeleteSkill()
```

### 3.3 NacosSkillSource

运行时实现：

```text
ListFrontmatters
LoadFrontmatter
LoadInstructions
ListResources
LoadResource
```

内部走 runtime 下载：

```text
/v3/client/ai/skills?name=&label=latest&md5=
```

本地缓存 ZIP，使用 MD5 与 304 避免重复下载。

### 3.4 SkillCodec

负责格式兼容：

1. 解析 `SKILL.md` frontmatter。
2. 容忍 Nacos 扩展字段：`skillSet/groups/keywords/modelName/modelDescription/matchHint/activation/priority`。
3. 扩展字段同时归入 `metadata`，方便 ADK 标准解析。
4. 生成标准 `SKILL.md`。
5. 支持资源目录：`references/assets/scripts/prompts/examples`。
6. 支持 ZIP 根目录 `SKILL.md` 和 `skill-name/SKILL.md`。

## 4. 生命周期映射

| ADK 状态 | Nacos 状态/动作 |
|---|---|
| draft | draft / editingVersion |
| pending_review | submit |
| published | publish + online |
| archived | offline |
| deleted | delete |
| private/public | scope PRIVATE/PUBLIC |
| tags/labels | bizTags / labels |

## 5. 优先级

| 优先级 | 范围 |
|---|---|
| P0 | Nacos runtime download API、ADK NacosSkillSource、SkillCodec、配置工厂 |
| P1 | 管理端 Save/List/Get/Delete 映射、Agent 自己保存 skill |
| P2 | 版本标签、发布审核、online/offline、Registry well-known |
| P3 | If-Match、灰度 label、搜索推荐、Agent/Skill 联动治理 |


## 本仓库范围修订

本仓库只包含 Nacos Server 侧实现与交付脚本。外部 ADK 的 Go 适配代码不放入本仓库，只保留 Runtime API 契约。
