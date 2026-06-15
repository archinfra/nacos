# SkillHub 适配矩阵

本文按“需求项 -> 当前源码状态 -> 建议动作”整理，方便检查项目是否适配。

| 模块 | 需求 | 当前建议状态 | 说明 |
|---|---|---|---|
| Nacos Admin API | `/v3/admin/ai/skills/list` | P0 已具备/需联调 | `SkillAdminController` 已有 list 能力。 |
| Nacos Admin API | get skill meta/version | P0 已具备/需联调 | `GET /v3/admin/ai/skills`、`GET /version`。 |
| Nacos Admin API | draft/update/delete | P0 已具备/需联调 | `POST/PUT/DELETE /draft`。 |
| Nacos Admin API | submit/publish/online/offline | P1 已具备/需联调 | 生命周期接口已存在。 |
| Nacos Admin API | labels/bizTags/metadata/scope | P1 已具备/需联调 | 表单字段需要前端和 ADK 对齐。 |
| Nacos ZIP | upload/batch/download | P0 已具备/需联调 | 需验证大 ZIP、安全限制、双结构 ZIP。 |
| Runtime API | 200 zip | P0 已具备/需联调 | `SkillClientController` 返回 ZIP。 |
| Runtime API | 304 + ETag + MD5 | P0 已具备/需联调 | 当前已存在 MD5 头；建议 ETag 使用标准 quoted 格式。 |
| Version Resolve | `version > label > latest` | P0 需要测试锁定 | 建议用测试固定优先级。 |
| Frontmatter | 扩展字段 | P0 已具备/需联调 | `SkillBase` 已出现扩展字段。 |
| Registry | well-known index/search/zip | P2 已具备/需开启配置 | 受 `nacos.ai.skill.registry.enabled=true` 控制。 |
| 并发控制 | `If-Match` | P3 部分具备 | AgentKit normalized registry API 已有；老 `/v3/admin/ai/skills` 表单接口建议后续补。 |
| ADK | NacosSkillSource | P0 待接入 | 本包新增 `adk-nacos-skillhub` Go 侧适配骨架。 |
| ADK | md5 cache/304 | P0 待接入 | 本包新增缓存与 runtime 下载示例。 |
| ADK | Save/List/Get/Delete | P1 待接入 | 本包新增 client/service 方法骨架。 |

## 推荐落地顺序

1. 先联调 `/v3/client/ai/skills`：ADK 能下载 ZIP、解析 SKILL.md、缓存、304。
2. 再联调 `/v3/admin/ai/skills/upload`：人工或 Agent 可以上传 Skill。
3. 再联调 draft/update/publish/online：支持 Agent 自沉淀 Skill。
4. 最后补 If-Match、灰度标签、搜索推荐、治理能力。
