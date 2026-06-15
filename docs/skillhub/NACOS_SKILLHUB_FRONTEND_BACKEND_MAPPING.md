# SkillHub 前后端接口映射

本文档用于前端控制台适配 Nacos SkillHub 后端。

## 页面建议

- Skill 列表页。
- Skill 详情页。
- Skill 版本页。
- Skill 上传页。
- Skill 标签/业务标签管理。
- Skill 发布/上线/下线操作。
- Skill Registry 下载测试页。

## 前端调用管理接口

### 列表

```http
GET /v3/console/ai/skills/list?namespaceId=public&pageNo=1&pageSize=20&skillName=&search=blur
```

### 详情

```http
GET /v3/console/ai/skills?namespaceId=public&skillName=demo-skill
```

### 版本详情

```http
GET /v3/console/ai/skills/version?namespaceId=public&skillName=demo-skill&version=v1
```

### 上传 ZIP

```http
POST /v3/console/ai/skills/upload
Content-Type: multipart/form-data

namespaceId=public
overwrite=true
targetVersion=v1
commitMsg=init
file=@demo-skill.zip
```

### 发布上线

```http
POST /v3/console/ai/skills/publish
POST /v3/console/ai/skills/online
```

## 注意

- 前端侧主要走 `/v3/console/ai/skills`。
- 核心业务最终落到 `/v3/admin/ai/skills`。
- Runtime 下载不要走 Console 接口，必须走 `/v3/client/ai/skills`。
- ZIP 下载接口返回二进制，不要按 JSON 解析。
- `304 Not Modified` 是正常响应，不要当成错误弹窗。
