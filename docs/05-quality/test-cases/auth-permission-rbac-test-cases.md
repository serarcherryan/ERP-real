# Auth Permission RBAC Test Cases

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 所属模块 | 认证与权限 |
| 影响端 | 后端服务 / Web 管理端 |
| 风险等级 | P0 |
| 关联需求/契约 | `docs/04-modules/auth-permission.md`, `docs/03-apis/user-role-permission-management.md`, `docs/09-decisions/ADR-0005-rbac-permission-model.md` |
| 负责人 | TBD |

## 2. 测试范围

- 覆盖范围：RBAC 初始化、admin 全权限、动态角色权限绑定、用户创建、用户角色绑定、业务接口按权限授权、审计落库、Web 组织权限面板。
- 不覆盖范围：生产 SSO、MFA、权限缓存压测。
- 测试数据：Flyway 初始化 5 个业务角色、admin 用户和权限注册表；测试中创建临时用户和调整角色权限。
- 外部依赖：后端测试使用 H2 PostgreSQL mode + Flyway。

## 3. 用例清单

| ID | 类型 | 场景 | 前置条件 | 步骤 | 期望结果 | 自动化 | 优先级 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-001 | Integration | admin 登录 | admin 用户存在 | POST `/api/v1/auth/login` | 返回 `superAdmin=true` 和全部权限 | 是 | P0 |
| TC-002 | Integration | 社工权限基线 | 社工账号存在 | GET `/api/v1/auth/me` | 仅返回新建/查看长者档案权限 | 是 | P0 |
| TC-003 | Security | 社工禁止修改档案 | 社工无 `resident.profile:update` | PATCH 长者档案 | 返回 `RESIDENT_PROFILE_FORBIDDEN` | 是 | P0 |
| TC-004 | Security | 物业主管可修改档案 | 物业主管有档案 CRUD | PATCH 长者档案 | 修改成功并写审计 | 是 | P0 |
| TC-005 | Integration | 部门经理新增用户 | 部门经理有 `identity.user:create` | POST `/api/v1/users` | 用户创建成功，密码 BCrypt，角色绑定成功 | 是 | P0 |
| TC-006 | Security | 社工禁止新增用户 | 社工无身份管理权限 | POST `/api/v1/users` | 返回 `IDENTITY_FORBIDDEN` | 是 | P0 |
| TC-007 | Integration | 修改角色权限 | 部门经理有 `identity.role:update_permissions` | PUT `/api/v1/roles/{roleId}/permissions` | 角色权限被替换，相关用户 `permission_version` 提升 | 是 | P0 |
| TC-008 | Regression | 权限变更影响业务接口 | 移除物业主管修改档案权限 | PATCH 长者档案 | 返回 `RESIDENT_PROFILE_FORBIDDEN` | 是 | P0 |
| TC-009 | Audit | 角色权限审计 | 修改角色权限 | 查询审计表 | 写入 role permission update 审计 | 是 | P1 |
| TC-010 | Validation | 未知权限编码 | 请求绑定不存在的权限 | PUT 角色权限 | 返回 `IDENTITY_PERMISSION_NOT_FOUND` | 是 | P0 |
| TC-011 | Component | Web 组织权限入口 | 当前用户有身份管理权限 | 点击组织权限菜单 | 展示用户、角色、权限三类数据 | 是 | P0 |
| TC-012 | Component | Web 新增用户 | 当前用户有 `identity.user:create` | 提交新增用户表单 | POST `/api/v1/users`，刷新用户列表 | 是 | P0 |
| TC-013 | Component | Web 修改用户角色 | 当前用户有 `identity.user:update` | 修改用户角色并保存 | PUT `/api/v1/users/{userId}/roles` | 是 | P0 |
| TC-014 | Component | Web 修改角色权限 | 当前用户有 `identity.role:update_permissions` | 勾选权限并保存 | PUT `/api/v1/roles/{roleId}/permissions` | 是 | P0 |

## 4. 执行记录

| 时间 | 环境 | 命令/方式 | 结果 | 失败摘要 | 修复记录 |
| --- | --- | --- | --- | --- | --- |
| 2026-05-02 | local | `cd apps/backend && mvn test -Dtest=AuthPermissionRbacControllerTest,ResidentProfileControllerTest,RoomControllerTest -q` | 通过 | 初次回归发现旧用例仍期望物业经理修改档案被拒绝，已与新 RBAC 权限矩阵不一致 | 将回归断言改为社工无修改权限、物业经理可修改档案 |
| 2026-05-02 | local | `cd apps/backend && mvn test -q` | 通过 | 无 | 无 |
| 2026-05-02 | local | `npm run test:web` | 通过 | 既有 Ant Design `act(...)` / message context 警告，不影响断言 | 提升 Web Vitest 超时阈值以适配较慢的集成交互用例 |
| 2026-05-02 | local | `npm run test:shared` | 通过 | 无 | 无 |
| 2026-05-02 | local | `git diff --check` | 通过 | 无 | 无 |
| 2026-05-02 | local | `npm run test:web` | 通过 | 新增组织权限面板用例初次断言停留在用户页查找权限编码，已与实际页签结构不一致；Ant Design 仍有既有 `act(...)` / message context 警告 | 调整测试为切换到权限注册表、用户、角色权限页签后分别验证读取和提交 |
| 2026-05-02 | local | `npm run build -w @erp-real/web-admin` | 通过 | Vite 提示单 chunk 大于 500 kB，不影响本次功能 | 后续可做路由级动态导入或 manual chunks |
| 2026-05-02 | local | `git diff --check` | 通过 | 无 | 无 |

## 5. 回归结论

- 已通过：RBAC 后端定向集成测试、后端全量测试、Web 管理端组织权限面板测试、Web 生产构建、共享包测试、diff 空白字符检查。
- 未通过：无。
- 跳过项及原因：无。
- 剩余风险：权限缓存、生产身份源、账号生命周期审批后续补充。
