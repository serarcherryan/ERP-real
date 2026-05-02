# Auth Login API Contract

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| API 名称 | Auth Login |
| 所属模块 | 认证与权限 |
| 负责人 | TBD |
| 版本 | v1 |
| 调用端 | Web 管理端 |
| 风险等级 | P0 |
| 数据敏感级别 | 敏感 |

## 2. Endpoint

```http
POST /api/v1/auth/login
GET /api/v1/auth/me
```

## 3. 业务说明

- 用户故事：后台用户输入用户名和密码后获取 JWT，Web 管理端用 JWT 访问后续业务接口。
- 前置条件：`sys_users` 中存在 enabled 用户，密码 hash 已初始化。
- 后置结果：登录成功返回 token 和用户上下文；登录失败返回错误并写入审计。
- 幂等要求：登录接口无业务写入幂等要求，失败审计允许追加。
- 权限要求：`/login` 公开；`/me` 需要 Bearer token。
- 审计要求：登录失败写 `audit_logs`，登录成功当前记录应用日志。

## 4. Request

### 4.1 POST /api/v1/auth/login

```json
{
  "username": "social_worker",
  "password": "Erp@2026"
}
```

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| username | string | 是 | 非空，匹配 `sys_users.username` | 登录用户名 |
| password | string | 是 | 非空 | 明文仅用于本次校验，不落库 |

### 4.2 GET /api/v1/auth/me

```http
Authorization: Bearer <jwt>
```

## 5. Response

### 5.1 LoginResponse

```json
{
  "token": "<jwt>",
  "user": {
    "userId": "user-social-worker",
    "displayName": "社工",
    "role": "social-worker",
    "tenantId": "tenant-yiyang",
    "facilityId": "facility-hecheng"
  }
}
```

### 5.2 MeResponse

```json
{
  "userId": "user-social-worker",
  "displayName": "社工",
  "role": "social-worker",
  "tenantId": "tenant-yiyang",
  "facilityId": "facility-hecheng"
}
```

## 6. Error Codes

| Code | HTTP | 场景 | 前端处理建议 |
| --- | --- | --- | --- |
| INVALID_INPUT | 400 | 用户名或密码为空 | 提示补齐输入 |
| AUTH_FAILED | 401 | 用户不存在、禁用或密码错误 | 提示用户名或密码错误 |
| AUTH_REQUIRED | 401 | 缺少 Bearer token 或 token 无效/过期 | 清理本地登录态并回登录页 |

## 7. 兼容策略

- JWT claims 当前包含 `sub`、`displayName`、`role`、`tenantId`、`facilityId`。
- 新增用户字段应向后兼容；变更 token 结构需更新 Web `AuthUser` 和后端 `JwtService`。
- 初始账号仅用于开发和首期演示，生产账号初始化策略需要单独固化。

## 8. 性能与限流

| 项 | 目标 |
| --- | --- |
| P95 | <= 300ms |
| P99 | <= 800ms |
| 限流键 | username / IP |
| 缓存策略 | 当前不缓存密码校验结果 |
| 后续增强 | 登录失败次数限制、账号锁定、验证码或 MFA |

## 9. 测试清单

- 登录成功返回 token 和 user
- 用户名为空
- 密码为空
- 用户不存在
- 密码错误
- disabled 用户不可登录
- JWT 可访问 `/api/v1/auth/me`
- 无 token 访问受保护接口返回 `AUTH_REQUIRED`
- 登录失败写审计
- 前端登录成功进入管理端
- 前端退出登录清理本地 token/user
