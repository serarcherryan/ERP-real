# Auth And Permission Module

## 1. 模块档案

| 字段 | 内容 |
| --- | --- |
| 模块名称 | 认证与权限 |
| 所属业务域 | 平台基础能力 |
| 负责人 | TBD |
| 业务优先级 | P0 |
| 首发端 | Web 管理端 / 后端服务 |
| 依赖模块 | 租户与机构、审计合规 |
| 下游影响 | 长者档案、房间管理、入住生活、审计合规 |

## 2. 业务目标

- 解决的问题：为 Web 管理端提供登录、用户身份识别、角色上下文和后端接口认证入口。
- 目标用户：社工、社工主管、部门经理、物业经理、物业主管。
- 成功指标：登录成功率、认证失败率、Token 过期导致的重登录率、越权访问拦截率。
- 不做范围：生产级 SSO、MFA、密码找回、账号生命周期审批、权限配置 UI。

## 3. 核心流程

```text
用户输入用户名/密码
  -> POST /api/v1/auth/login
  -> 后端读取 sys_users
  -> BCrypt 校验密码
  -> 签发 8 小时 JWT
  -> Web 保存 token/user 到 localStorage
  -> 后续 apiFetch 自动携带 Authorization: Bearer <token>
  -> AuthFilter 解析 JWT 并写入 AuthContext
  -> 业务模块从 RequestContext 读取 tenantId/facilityId/role/userId
```

## 4. 领域模型

| 实体/聚合 | 说明 | 所有者 | 生命周期 |
| --- | --- | --- | --- |
| SysUser | 后台用户账号 | 认证与权限 | Enabled / Disabled |
| AuthenticatedUser | 请求级认证上下文 | 认证与权限 | Request scoped |
| JwtToken | 登录后签发的无状态访问令牌 | 认证与权限 | Issued / Expired |
| Role | 角色标识，当前与共享领域角色保持一致 | 认证与权限 | Active |

当前角色：

| 角色 | 部门 |
| --- | --- |
| social-worker | 养老部门 |
| social-worker-supervisor | 养老部门 |
| department-manager | 养老部门 |
| property-manager | 物业部门 |
| property-supervisor | 物业部门 |

## 5. 数据模型

| 表 | 用途 | 关键字段 | 索引 |
| --- | --- | --- | --- |
| sys_users | 后台登录账号 | id, username, password_hash, display_name, role, tenant_id, facility_id, enabled | username unique, tenant_id + facility_id |
| audit_logs | 登录失败审计 | module_name=auth, action=LOGIN_FAILED, operator, resource_id, detail | resource, created_at |

当前迁移：

- `V4__sys_users.sql`：创建 `sys_users`，初始化 5 个角色账号。
- 初始密码：`Erp@2026`。

## 6. API 与事件

| 类型 | 名称 | 调用方/订阅方 | 契约文件 |
| --- | --- | --- | --- |
| API | Auth Login | Web 管理端 | `docs/03-apis/auth-login.md` |

当前无异步事件。

## 7. 权限与数据范围

| 操作 | 认证 | 数据范围 | 审计 |
| --- | --- | --- | --- |
| 登录 | 公开接口，用户名/密码校验 | 返回账号绑定的 tenantId/facilityId/role | 登录失败写审计 |
| 获取当前用户 | Bearer JWT 或 dev token | 当前 token 内的用户上下文 | 否 |
| 访问业务接口 | Bearer JWT 或 dev token | token 内 tenantId/facilityId | 由业务模块决定 |

## 8. 安全与一致性

- 密码使用 BCrypt hash 存储，不存明文密码。
- JWT 使用 `erp.auth.jwt-secret` 签名，当前有效期为 8 小时。
- `prod` 环境必须通过环境变量或安全配置提供 JWT secret。
- Web 当前将 token 和 user 信息保存到 `localStorage`。
- `dev/test` 仍保留静态 Bearer token fallback，用于开发联调和既有房间管理测试。
- 当前 Spring Security 由自定义 `AuthFilter` 承担认证，`/api/v1/auth/login` 和 `/actuator/**` 放行。

## 9. 可观测性

| 类型 | 内容 |
| --- | --- |
| 日志 | 登录成功 username/role，登录失败 username/IP |
| 审计 | 登录失败写入 `audit_logs` |
| 指标 | 后续接入登录成功率、失败率、401 比例 |
| 告警 | 后续接入异常失败率、单用户/单 IP 连续失败 |

## 10. 测试计划

- 后端集成测试：登录成功、密码错误、禁用账号、JWT 访问 `/me`、无 token 访问业务接口。
- 前端组件测试：未登录展示登录页，登录成功进入管理界面，退出清理本地状态。
- 安全测试：密码不进入日志、token 过期返回 401、跨租户 token 不可访问其他机构。
- 回归测试：房间管理等业务接口继续从认证上下文读取租户、机构和角色。

## 11. 发布计划

- 数据迁移：执行 `V4__sys_users.sql` 创建账号表和初始账号。
- 配置变更：生产必须配置 `ERP_AUTH_JWT_SECRET` 或等价安全配置。
- 回滚方式：回滚到静态 token 仅可用于开发环境，生产不应启用。
- 后续演进：生产身份源、密码策略、MFA、账号锁定、刷新 token 和权限管理 UI 需要独立设计。
