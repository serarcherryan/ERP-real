# ADR-0005: RBAC Permission Model

| 字段 | 内容 |
| --- | --- |
| 状态 | Accepted |
| 日期 | 2026-05-02 |
| 决策人 | TBD |
| 影响范围 | 认证与权限、长者档案、房间管理、Web 管理端 |

## Context

当前后端通过 `sys_users.role` 和业务服务内的硬编码角色列表授权。随着新增用户、角色、权限管理能力，角色与权限之间的绑定必须可动态调整，并且需要 admin 用户拥有所有权限。长者档案、身份管理和后续 P0 模块都需要统一授权入口、审计和租户隔离。

## Decision

采用 RBAC 2.0 模型：`sys_users` 保留主角色兼容字段，同时新增 `roles`、`permissions`、`user_roles`、`role_permissions`。业务接口不再直接判断角色字符串，而是通过 `PermissionService` 校验权限编码。

核心权限编码使用 `domain.resource:action` 格式，例如：

- `resident.profile:create`
- `resident.profile:read`
- `resident.profile:update`
- `resident.profile:delete`
- `identity.user:create`
- `identity.role:update_permissions`

JWT 只携带身份上下文和 `permissionVersion`，不作为最终权限准入来源。admin 用户通过 `is_super_admin=true` 获得所有权限，避免角色权限误配置导致管理员被锁死。

## Options Considered

| 方案 | 优点 | 缺点 | 结论 |
| --- | --- | --- | --- |
| 继续硬编码角色判断 | 改动小 | 无法动态调整权限，难审计，跨模块重复 | 放弃 |
| 纯 RBAC | 简洁、成熟、适合当前角色权限矩阵 | 对复杂条件授权支持有限 | 采用 |
| ABAC/策略引擎 | 表达力强，可覆盖复杂数据条件 | 首期复杂度高，运维和调试成本高 | 后续演进 |

## Consequences

- 正面影响：角色权限可动态调整，业务代码按权限编码解耦，admin 全权限可控。
- 负面影响：每次授权需要查询权限服务或缓存，权限变更需要处理缓存失效。
- 成本：新增 Flyway 迁移、JPA 实体、权限服务、身份管理 API 和回归测试。
- 后续工作：前端权限管理 UI、权限缓存、账号锁定、MFA、生产身份源。

## Validation

- 如何证明这个决策有效：后端测试覆盖 admin 全权限、社工越权、新增用户、修改角色权限、权限变更影响业务接口。
- 需要观察的指标：403 比例、权限查询 P95、权限变更审计覆盖率、权限缓存命中率。
- 需要复盘的时间点：权限管理 UI 首次灰度后 7 天。
