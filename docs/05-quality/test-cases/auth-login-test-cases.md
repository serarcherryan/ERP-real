# Auth Login Test Cases

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 所属模块 | 认证与权限 |
| 影响端 | 后端服务 / Web 管理端 |
| 风险等级 | P0 |
| 关联需求/契约 | `docs/04-modules/auth-permission.md`, `docs/03-apis/auth-login.md` |
| 负责人 | TBD |

## 2. 测试范围

- 覆盖范围：登录页展示、用户名密码登录、JWT 签发、用户上下文保存、认证失败、退出登录、业务接口认证上下文。
- 不覆盖范围：生产 SSO、MFA、账号锁定、密码找回、刷新 token。
- 测试数据：`V4__sys_users.sql` 初始化的 5 个角色账号，初始密码 `Erp@2026`。
- 外部依赖：后端测试使用 H2 PostgreSQL mode + Flyway；Web 测试使用 Vitest/jsdom。

## 3. 用例清单

| ID | 类型 | 场景 | 前置条件 | 步骤 | 期望结果 | 自动化 | 优先级 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-AUTH-001 | Integration | 登录成功 | sys_users 存在 enabled 用户 | POST `/api/v1/auth/login` | 返回 JWT 和 user | 待补 | P0 |
| TC-AUTH-002 | Integration | 密码错误 | sys_users 存在用户 | POST 错误密码 | 返回 AUTH_FAILED，写失败审计 | 待补 | P0 |
| TC-AUTH-003 | Integration | 空用户名/密码 | 无 | POST 空字段 | 返回 INVALID_INPUT | 待补 | P0 |
| TC-AUTH-004 | Integration | JWT 访问当前用户 | 登录成功 | GET `/api/v1/auth/me` | 返回 token 内用户上下文 | 待补 | P0 |
| TC-AUTH-005 | Security | 无 token 访问业务接口 | 未登录 | GET `/api/v1/rooms/tree` | 返回 AUTH_REQUIRED | 是 | P0 |
| TC-AUTH-006 | Frontend | 未登录展示登录页 | localStorage 无 token/user | 打开 Web | 展示登录表单 | 待补 | P0 |
| TC-AUTH-007 | Frontend | 登录成功进入管理端 | 后端可用 | 输入账号密码并提交 | 保存 token/user，进入长者档案页 | 待补 | P0 |
| TC-AUTH-008 | Frontend | 退出登录 | 已登录 | 点击退出 | 清理 token/user，回登录页 | 待补 | P0 |
| TC-AUTH-009 | Security | token 过期 | 使用过期 JWT | 访问业务接口 | 返回 401，前端清理登录态 | 待补 | P0 |

## 4. 执行记录

| 时间 | 环境 | 命令/方式 | 结果 | 失败摘要 | 修复记录 |
| --- | --- | --- | --- | --- | --- |
| 2026-04-26 18:43 CST | local | `cd apps/backend && mvn test` | Pass | 首次执行因 test profile 缺少 `erp.auth.jwt-secret` 导致 Spring Context 启动失败 | 已在 `apps/backend/src/test/resources/application-test.yml` 补充测试 JWT secret；重跑后 9/9 通过，包含无 token 业务接口拦截 |
| 2026-04-26 18:43 CST | local | `npm test` | Pass | 首次执行时 Web 管理端测试未注入登录态，渲染停留在登录页；重跑仍有 Ant Design `act(...)` 非阻断警告 | 已更新 `apps/web-admin/src/App.test.tsx`，按角色写入 `erp_token`/`erp_user` 后再断言；shared-domain 10/10，web-admin 3/3 |
| 2026-04-26 18:44 CST | local | `cd apps/backend && mvn package` | Pass | 无阻塞失败；存在 Spring Security generated password、Flyway H2 版本、JDK native access 非阻断警告 | 后端测试 9/9 通过并生成 `target/backend-0.1.0.jar` |
| 2026-04-26 18:45 CST | local | `npm run build` | Pass | Vite 提示入口 chunk 超过 500 kB，非阻断 | shared-domain TypeScript 构建通过，web-admin TypeScript + Vite 构建通过 |

## 5. 回归结论

- 已通过：现有业务接口认证拦截、前端现有回归测试、共享领域测试。
- 未通过：无。
- 跳过项及原因：登录 API 成功/失败、`/me`、前端登录交互尚未补专门自动化用例。
- 剩余风险：生产身份源、密码策略、账号锁定、MFA、刷新 token、JWT secret 管理仍需后续固化。
