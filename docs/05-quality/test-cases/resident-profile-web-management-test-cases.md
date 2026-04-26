# Resident Profile Web Management Test Cases

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 所属模块 | 长者档案管理 |
| 影响端 | Web 管理端 |
| 风险等级 | P0 |
| 关联需求/契约 | `docs/04-modules/resident-profile.md`, `docs/03-apis/resident-profile-management.md` |
| 负责人 | TBD |

## 2. 测试范围

- 覆盖范围：长者档案列表、角色切换、创建/编辑入口、详情查看、字段脱敏、机构过滤、导出权限提示、暗色/亮色主题切换。
- 不覆盖范围：真实后端持久化、真实审计落库、真实导出任务生成、完整医疗病历和财务数据。
- 测试数据：合成多机构、多角色、多状态长者档案，不使用真实身份证或手机号。
- 外部依赖：首版使用前端 mock 数据；后端接入后补契约测试和接口集成测试。

## 3. 用例清单

| ID | 类型 | 场景 | 前置条件 | 步骤 | 期望结果 | 自动化 | 优先级 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-001 | Unit | 物业角色敏感字段脱敏 | 角色为物业主管 | 生成长者视图 | 手机、证件、联系人电话均脱敏 | 是 | P0 |
| TC-002 | Unit | 社工主管查看敏感字段 | 角色为社工主管 | 生成长者视图 | 展示明文字段，后端接入后需写审计 | 是 | P0 |
| TC-003 | Unit | 角色权限矩阵 | 五类角色存在 | 检查创建、修改、导出权限 | 养老执行角色可创建/修改，物业角色默认只读或导出 | 是 | P0 |
| TC-004 | Unit | 机构过滤 | 多机构 mock 数据 | 按 facilityId 查询 | 不返回其他机构档案 | 是 | P0 |
| TC-005 | Component | 长者档案工作台渲染 | 默认社工主管 | 打开 Web 管理端 | 展示标题、列表和关键操作入口 | 是 | P0 |
| TC-006 | Component | 物业主管禁用新建 | 切换到物业主管 | 查看新建按钮 | 新建按钮禁用 | 是 | P0 |
| TC-007 | Unit | 入住摘要扁平化 | 共享领域模型存在 admission | 生成列表视图 | room、bed、careLevel 与 admission 摘要一致 | 是 | P0 |
| TC-008 | Manual | 详情抽屉 | 列表存在数据 | 点击查看按钮 | 展示档案摘要、入住信息、联系人、健康风险、完整度、时间线 Tab | 否 | P0 |
| TC-009 | Manual | 档案完整度和风险展示 | 多状态 mock 数据 | 查看列表和详情 | 展示完整度、待补字段、跌倒/压疮风险 | 否 | P0 |
| TC-010 | Manual | 导出审批提示 | 角色为部门经理或物业经理 | 点击导出 | 提示进入导出审批流程 | 否 | P1 |
| TC-011 | Component | 主题切换 | 默认暗色主题 | 点击主题切换按钮 | 页面切换到亮色主题，业务按钮权限不变 | 是 | P1 |
| TC-012 | Manual | 外部字体降级 | 网络不可用或字体加载失败 | 打开 Web 管理端 | 页面仍可用，使用系统字体降级 | 否 | P2 |
| TC-013 | Future Contract | 幂等创建 | 后端 API 接入 | 重复提交相同 Idempotency-Key | 只创建一条档案 | 否 | P0 |
| TC-014 | Future Security | 审计落库 | 后端审计接入 | 查看明文敏感字段、修改、导出 | 生成审计日志 | 否 | P0 |

## 4. 执行记录

| 时间 | 环境 | 命令/方式 | 结果 | 失败摘要 | 修复记录 |
| --- | --- | --- | --- | --- | --- |
| 2026-04-26 | local | `npm test` | Pass | 首次失败：JSDOM 缺少 `matchMedia`，角色下拉测试命中多个 combobox | 已补浏览器 API stub，并收窄测试选择器后复跑通过，7/7 tests passed |
| 2026-04-26 | local | `npm run build` | Pass | 首次失败：`vite.config.ts` 中 test 字段类型不被生产构建识别 | 拆分 `vitest.config.ts` 后复跑通过；仍有 Ant Design chunk size warning |
| 2026-04-26 | local | `npm test` | Pass | monorepo 迁移后复跑 | `packages/shared-domain` 5/5，`apps/web-admin` 2/2，通过 |
| 2026-04-26 | local | `npm run build` | Pass | monorepo 迁移后首次失败：shared project reference 不能禁用 emit，缺少 Node 类型 | 共享包改为只产出声明文件，补 `@types/node` 并调整 workspace 顺序后复跑通过；仍有 Ant Design chunk size warning |
| 2026-04-26 | local | `npm test` | Pass | 扩展长者档案字段、健康风险、完整度后复跑 | `packages/shared-domain` 6/6，`apps/web-admin` 2/2，通过 |
| 2026-04-26 | local | `npm run build` | Pass | 扩展长者档案字段、健康风险、完整度后复跑 | 构建通过；仍有 Ant Design chunk size warning |
| 2026-04-26 | local | `npm test` | Pass | 前端重设计后补充主题切换测试并复跑 | `packages/shared-domain` 6/6，`apps/web-admin` 3/3，通过；仍有 Ant Design act warning |
| 2026-04-26 | local | `npm run build` | Pass | 前端重设计后复跑 | 构建通过；仍有 Ant Design chunk size warning |

## 5. 回归结论

- 已通过：权限/脱敏/机构过滤单测，Web 工作台渲染和物业主管只读入口组件测试，生产构建。
- 未通过：无。
- 跳过项及原因：后端持久化、真实审计、真实导出任务尚未实现。
- 剩余风险：前端 mock 无法证明服务端权限、租户隔离和审计落库，后端接入时必须补 Contract、Integration 和 Security 测试；Web 管理端首包仍需后续代码分割优化。
