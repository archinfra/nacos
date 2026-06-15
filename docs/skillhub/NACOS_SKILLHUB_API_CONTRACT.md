# Nacos SkillHub API Contract

## 1. 统一响应

管理接口默认使用 Nacos v2 Result 包装：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

Runtime 下载接口不使用 Result 包装，直接返回 ZIP 或 304。

## 2. Admin API

### 2.1 列表

```http
GET /v3/admin/ai/skills/list?namespaceId=public&pageNo=1&pageSize=20&skillName=dialogue&search=blur
```

返回：

```json
{
  "code": 0,
  "data": {
    "pageNumber": 1,
    "pagesAvailable": 1,
    "totalCount": 1,
    "pageItems": [
      {
        "namespaceId": "public",
        "name": "novel-dialogue-card",
        "description": "对话场景卡提取 Skill",
        "enable": true,
        "scope": "PUBLIC",
        "labels": {"latest":"v1","stable":"v1"},
        "editingVersion": null,
        "reviewingVersion": null,
        "onlineCnt": 1,
        "downloadCount": 12
      }
    ]
  }
}
```

### 2.2 获取 Skill 元信息

```http
GET /v3/admin/ai/skills?namespaceId=public&skillName=novel-dialogue-card
```

### 2.3 获取指定版本

```http
GET /v3/admin/ai/skills/version?namespaceId=public&skillName=novel-dialogue-card&version=v1
```

### 2.4 创建草稿

```http
POST /v3/admin/ai/skills/draft
Content-Type: application/x-www-form-urlencoded

namespaceId=public&skillName=novel-dialogue-card&targetVersion=v2&skillCard={...}&commitMsg=init
```

### 2.5 更新草稿

```http
PUT /v3/admin/ai/skills/draft
Content-Type: application/x-www-form-urlencoded
If-Match: <resourceVersion-or-digest-optional-future>

namespaceId=public&skillName=novel-dialogue-card&version=v2&skillCard={...}&commitMsg=update
```

> 当前老表单接口建议后续补 `If-Match`。如果要立即做 JSON + If-Match，可优先走 normalized registry API。

### 2.6 提交、发布、上线

```http
POST /v3/admin/ai/skills/submit
POST /v3/admin/ai/skills/publish
POST /v3/admin/ai/skills/online
POST /v3/admin/ai/skills/offline
```

参数均使用 form：

```text
namespaceId=public&skillName=novel-dialogue-card&version=v1
```

### 2.7 标签、业务标签、元数据、可见范围

```http
PUT /v3/admin/ai/skills/labels
PUT /v3/admin/ai/skills/biz-tags
PUT /v3/admin/ai/skills/metadata
PUT /v3/admin/ai/skills/scope
```

labels 示例：

```text
namespaceId=public&skillName=novel-dialogue-card&labels={"latest":"v2","stable":"v1"}
```

metadata 示例：

```text
namespaceId=public&skillName=novel-dialogue-card&skillSet=novel&groups=["dialogue"]&keywords=["对话","话语权"]&activation=on_intent&priority=100
```

### 2.8 ZIP 上传

```http
POST /v3/admin/ai/skills/upload
Content-Type: multipart/form-data

namespaceId=public
overwrite=true
targetVersion=v1
commitMsg=init
file=@novel-dialogue-card.zip
```

批量上传：

```http
POST /v3/admin/ai/skills/upload/batch
Content-Type: multipart/form-data

namespaceId=public
overwrite=true
file=@skills-batch.zip
```

### 2.9 ZIP 下载

```http
GET /v3/admin/ai/skills/version/download?namespaceId=public&skillName=novel-dialogue-card&version=v1
```

返回：

```text
200 application/zip
Content-Disposition: attachment;filename=novel-dialogue-card.zip
```

## 3. Runtime Download API

```http
GET /v3/client/ai/skills?namespaceId=public&name=novel-dialogue-card&label=latest&md5=<local-md5>
```

### 3.1 首次下载

```http
200 OK
Content-Type: application/zip
ETag: <md5>
X-Nacos-Skill-Md5: <md5>
X-Nacos-Skill-Resolved-Version: v1
```

### 3.2 本地缓存命中

```http
304 Not Modified
ETag: <md5>
X-Nacos-Skill-Md5: <md5>
X-Nacos-Skill-Resolved-Version: v1
```

### 3.3 版本解析

优先级：

```text
version > label > latest
```

建议 ADK 调用：

```text
生产环境：label=stable
灰度环境：label=gray
开发环境：label=latest
锁定版本：version=v1
```

## 4. Registry Compatibility API

```http
GET /registry/{namespaceId}/.well-known/agent-skills/index.json
GET /registry/{namespaceId}/.well-known/agent-skills/{name}/SKILL.md
GET /registry/{namespaceId}/.well-known/agent-skills/{name}.zip
GET /registry/{namespaceId}/api/search?q=dialogue&limit=20
```

该接口只暴露公开、上线、启用的 Skill。

## 5. ZIP 结构

支持结构一：

```text
SKILL.md
references/...
assets/...
scripts/...
prompts/...
examples/...
```

支持结构二：

```text
skill-name/SKILL.md
skill-name/references/...
skill-name/assets/...
skill-name/scripts/...
skill-name/prompts/...
skill-name/examples/...
```

## 6. Frontmatter 扩展字段

```yaml
---
name: novel-dialogue-card
description: 提取对话场景卡
version: v1
skillSet: novel
groups: [dialogue, craft]
keywords: [对话, 话语权, 试探, 压迫]
modelName: 对话场景卡提取器
modelDescription: 专门分析对话权力关系和可迁移写法
matchHint: 当用户要求拆解对话描写时启用
activation: on_intent
priority: 100
metadata:
  skillSet: novel
  groups: [dialogue, craft]
  keywords: [对话, 话语权, 试探, 压迫]
---
```
