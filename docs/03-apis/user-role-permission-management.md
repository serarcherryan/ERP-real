# User Role Permission Management API Contract

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| API 名称 | User Role Permission Management |
| 所属模块 | 认证与权限 |
| 负责人 | TBD |
| 版本 | v1 |
| 调用端 | Web 管理端 |
| 风险等级 | P0 |
| 数据敏感级别 | 高敏感 |

## 2. Endpoint

```http
GET /api/v1/permissions
GET /api/v1/roles
GET /api/v1/roles/{roleId}
PUT /api/v1/roles/{roleId}/permissions
GET /api/v1/users
POST /api/v1/users
GET /api/v1/users/{userId}
PUT /api/v1/users/{userId}/roles
POST /api/v1/users/{userId}/disable
```

## 3. 业务说明

- 用户故事：部门经理、物业经理和 admin 可以新增后台用户，并动态调整角色与权限绑定。
- 前置条件：调用方已认证，且具备对应 `identity.*` 权限。
- 后置结果：用户、角色或角色权限变更落库，并写入审计日志。
- 幂等要求：用户创建按 `tenantId + username` 防重；角色权限替换为幂等 PUT。
- 权限要求：查看角色/权限需要 `identity.role:read`；新增用户需要 `identity.user:create`；修改角色权限需要 `identity.role:update_permissions`。
- 审计要求：新增用户、禁用用户、修改用户角色、修改角色权限必须写入 `audit_logs`，不得记录密码明文。

## 4. Request

### 4.1 POST /api/v1/users

```json
{
  "username": "new_social_worker",
  "password": "Erp@2026",
  "displayName": "新社工",
  "roleIds": ["role-social-worker"]
}
```

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| username | string | 是 | 同租户唯一，1-80 字符 | 登录名 |
| password | string | 是 | 8-128 字符 | 仅本次入参，BCrypt 后落库 |
| displayName | string | 是 | 1-120 字符 | 展示名 |
| roleIds | array | 是 | 至少 1 个，必须属于当前租户 | 用户角色 |

### 4.2 PUT /api/v1/roles/{roleId}/permissions

```json
{
  "permissionCodes": [
    "resident.profile:create",
    "resident.profile:read",
    "resident.profile:update"
  ]
}
```

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| permissionCodes | array | 是 | 权限编码必须存在 | 替换该角色全部权限绑定 |

## 5. Response

```json
{
  "data": {
    "id": "role-social-worker",
    "code": "social-worker",
    "name": "社工",
    "permissionCodes": [
      "resident.profile:create",
      "resident.profile:read"
    ]
  },
  "traceId": "trace-id"
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data.id | string | 用户或角色 ID |
| data.code | string | 角色编码或权限编码 |
| data.permissionCodes | array | 当前绑定的权限编码 |
| traceId | string | 链路追踪 ID |

## 6. Error Codes

| Code | HTTP | 场景 | 前端处理建议 |
| --- | --- | --- | --- |
| IDENTITY_FORBIDDEN | 403 | 缺少用户/角色权限管理权限 | 隐藏入口或提示联系管理员 |
| IDENTITY_USER_DUPLICATED | 409 | 用户名重复 | 提示更换用户名 |
| IDENTITY_ROLE_NOT_FOUND | 404 | 角色不存在或跨租户 | 刷新角色列表 |
| IDENTITY_PERMISSION_NOT_FOUND | 400 | 权限编码不存在 | 刷新权限注册表 |
| IDENTITY_SUPER_ADMIN_PROTECTED | 403 | 尝试禁用超级管理员 | 提示无权操作 |
| IDENTITY_SELF_DISABLE | 403 | 尝试禁用自己 | 提示无法禁用自身账号 |
| IDENTITY_SELF_ROLE_CHANGE | 403 | 尝试修改自己的角色 | 提示无法修改自身角色 |
| AUTH_REQUIRED | 401 | 未认证或 token 失效 | 回登录页 |

## 7. 兼容策略

- 权限编码只能新增，不允许静默改名或删除。
- 角色名称可调整，角色编码变更必须迁移用户绑定和前端引用。
- 响应新增字段必须向后兼容。

## 8. 性能与限流

| 项 | 目标 |
| --- | --- |
| P95 | <= 300ms |
| P99 | <= 800ms |
| QPS 预估 | 单机构 10 QPS |
| 限流键 | tenant_id / user_id / IP |
| 缓存策略 | 权限注册表和用户权限可短 TTL 缓存；角色权限更新必须清理缓存或提升 `permission_version` |

## 9. 测试清单

- admin 登录返回所有权限
- 部门经理/物业经理可新增用户
- 社工不可新增用户
- 部门经理/物业经理可修改角色权限
- 角色权限调整后影响业务接口授权
- 用户名重复返回冲突
- 权限编码不存在返回错误
- 修改角色权限写审计
- 禁止禁用超级管理员（返回 `IDENTITY_SUPER_ADMIN_PROTECTED`）
- 禁止用户禁用自身（返回 `IDENTITY_SELF_DISABLE`）
- 禁止用户修改自身角色（返回 `IDENTITY_SELF_ROLE_CHANGE`）
