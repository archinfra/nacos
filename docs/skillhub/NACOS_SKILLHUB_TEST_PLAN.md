# SkillHub 测试计划

## P0 测试

### 1. Runtime 下载 200

```bash
curl -i -L \
  "http://127.0.0.1:8848/v3/client/ai/skills?namespaceId=public&name=novel-dialogue-card&label=latest" \
  -o skill.zip
```

验收：

```text
HTTP 200
Content-Type: application/zip
ETag 存在
X-Nacos-Skill-Md5 存在
X-Nacos-Skill-Resolved-Version 存在
skill.zip 可解压
包含 SKILL.md
```

### 2. Runtime 下载 304

```bash
MD5=<上一步 X-Nacos-Skill-Md5>
curl -i -L \
  "http://127.0.0.1:8848/v3/client/ai/skills?namespaceId=public&name=novel-dialogue-card&label=latest&md5=${MD5}"
```

验收：

```text
HTTP 304
body 为空
ETag / X-Nacos-Skill-Md5 与本地一致
```

### 3. ZIP 上传双结构

结构一：

```text
SKILL.md
references/a.md
```

结构二：

```text
novel-dialogue-card/SKILL.md
novel-dialogue-card/references/a.md
```

上传：

```bash
curl -X POST "http://127.0.0.1:8848/v3/admin/ai/skills/upload" \
  -F "namespaceId=public" \
  -F "overwrite=true" \
  -F "targetVersion=v1" \
  -F "commitMsg=init" \
  -F "file=@skill.zip"
```

### 4. Frontmatter 扩展字段

SKILL.md：

```yaml
---
name: novel-dialogue-card
description: 对话场景卡
skillSet: novel
groups: [dialogue]
keywords: [对话, 话语权]
modelName: 对话技能
modelDescription: 分析对话权力关系
matchHint: 用户要拆解对话时使用
activation: on_intent
priority: 100
---
```

验收：

```text
Nacos 管理端字段可见
ADK SkillCodec 不丢字段
metadata 中保留扩展字段
```

## P1 测试

1. CreateDraft -> GetVersion。
2. UpdateDraft -> GetVersion 内容更新。
3. Submit -> 状态进入 reviewing。
4. Publish -> label latest 指向版本。
5. Online -> runtime 可下载。
6. Offline -> runtime 不可下载。
7. DeleteDraft -> editingVersion 清空。

## P2 测试

1. Registry index 只返回 PUBLIC + online + enable。
2. Registry search 支持 q 和 limit。
3. `{name}.zip` 可直接下载。
4. `{name}/SKILL.md` 可直接查看。

## P3 测试

1. If-Match 成功更新。
2. If-Match 不匹配返回 409。
3. gray label 指向灰度版本。
4. stable/latest 分别解析正确。
5. Agent 运行期间保存 Skill 后，本地 cache 被 invalidate。
