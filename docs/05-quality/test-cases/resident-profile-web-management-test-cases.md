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

- 覆盖范围：长者档案列表 API 加载、角色权限、创建入口、详情 API 加载、字段脱敏展示、导出权限提示、暗色/亮色主题切换。
- 不覆盖范围：真实导出任务生成、完整医疗病历和财务数据。
- 测试数据：自动化组件测试使用后端 API 响应替身，不使用真实身份证或手机号；后端集成测试使用 H2 + Flyway 合成数据。
- 外部依赖：Web 管理端通过 `/api/v1/residents` 与后端契约集成；组件测试 mock `fetch` 响应，后端契约由 Maven 集成测试覆盖。

## 3. 用例清单

| ID | 类型 | 场景 | 前置条件 | 步骤 | 期望结果 | 自动化 | 优先级 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-001 | Unit | 物业角色敏感字段脱敏 | 角色为物业主管 | 生成长者视图 | 手机、证件、联系人电话均脱敏 | 是 | P0 |
| TC-002 | Unit | 社工主管查看敏感字段 | 角色为社工主管 | 生成长者视图 | 展示明文字段，后端接入后需写审计 | 是 | P0 |
| TC-003 | Unit | 角色权限矩阵 | 五类角色存在 | 检查创建、修改、导出权限 | 养老执行角色可创建/修改，物业角色默认只读或导出 | 是 | P0 |
| TC-004 | Integration | 机构过滤 | 后端 API 可用 | 按 facilityId 查询 | 不返回其他机构档案 | 是 | P0 |
| TC-005 | Component | 长者档案工作台渲染 | 默认社工主管，列表 API 返回数据 | 打开 Web 管理端 | 展示标题、后端列表和关键操作入口 | 是 | P0 |
| TC-006 | Component | 物业主管禁用新建 | 切换到物业主管 | 查看新建按钮 | 新建按钮禁用 | 是 | P0 |
| TC-007 | Unit | 入住摘要扁平化 | 共享领域模型存在 admission | 生成列表视图 | room、bed、careLevel 与 admission 摘要一致 | 是 | P0 |
| TC-008 | Component | 详情抽屉 | 后端详情 API 返回数据 | 点击查看按钮 | 展示后端档案摘要、入住信息、联系人、健康风险、完整度、时间线 Tab；时间线展示 `YYYY-MM-DD HH:mm:ss` 和操作人名称 | 是 | P0 |
| TC-009 | Component | 档案完整度和风险展示 | 后端列表/详情 API 返回风险摘要 | 查看列表和详情 | 展示完整度、用户可理解的待补字段名、跌倒风险 | 是 | P0 |
| TC-010 | Manual | 导出审批提示 | 角色为部门经理或物业经理 | 点击导出 | 提示进入导出审批流程 | 否 | P1 |
| TC-011 | Component | 亮暗主题 | 默认打开登录页和工作台 | 页面默认亮色，点击主题切换按钮后进入暗色，业务按钮权限不变 | 是 | P1 |
| TC-012 | Manual | 外部字体降级 | 网络不可用或字体加载失败 | 打开 Web 管理端 | 页面仍可用，使用系统字体降级 | 否 | P2 |
| TC-013 | Future Contract | 幂等创建 | 后端 API 接入 | 重复提交相同 Idempotency-Key | 只创建一条档案 | 否 | P0 |
| TC-014 | Component | 新建档案格式校验 | 社工主管打开新建档案 | 输入错误档案号、证件号并点击下一页 | 表单展示对应“XX格式不正确”，不调用创建 API；房间床位只校验必填，不校验格式 | 是 | P0 |
| TC-014A | Component | 身份证自动推导出生日期和实时年龄 | 社工主管打开新建档案 | 输入合法居民身份证号 | 前端自动展示出生日期，并按当前日期从出生日期实时计算年龄；保存时提交推导出的 `birthDate`，不提交年龄字段 | 是 | P0 |
| TC-015 | Component | 档案列表加载失败 | 后端列表 API 返回 500 | 打开长者档案工作台 | 页面展示“长者档案加载失败”、错误详情和重试入口 | 是 | P0 |
| TC-016 | Integration | 按房间位置搜索档案 | 后端 API 可用且入住信息包含房间/位置 | 使用房间关键字查询 `/api/v1/residents` | 返回匹配档案，空房间字段不会导致数据库 500 | 是 | P0 |
| TC-017 | Integration | 无关键字加载档案列表 | 后端 API 可用且存在档案 | 不传 keyword 查询 `/api/v1/residents` | 返回档案列表，PostgreSQL 不会因空 keyword 参数推断为 bytea 而 500 | 是 | P0 |
| TC-018 | Component | 查看档案后编辑 | 社工主管打开长者档案 | 点击编辑，按档案摘要、入住信息、联系人、健康风险分页修改健康摘要和责任社工并保存 | 编辑弹窗高度控制在浏览器内，前几页主按钮为“下一页”，最后一页主按钮为“保存”；编辑弹窗层级高于右侧详情抽屉；前端调用 `PATCH /api/v1/residents/{id}`，提交当前版本号和完整可编辑档案字段 | 是 | P0 |
| TC-019 | Component | 详情不展示模拟数据 | 后端详情字段为空或不存在 | 打开详情抽屉 | 不展示前端合成的服务时间线、风险等级、房间床位或护理等级；仅用“未填写”作为展示占位 | 是 | P0 |
| TC-020 | Future Security | 审计落库 | 后端审计接入 | 查看明文敏感字段、修改、导出 | 生成审计日志 | 否 | P0 |
| TC-021 | Component / Contract | 删除个案经理和压疮风险 | 后端列表/详情 API 返回新契约 | 查看列表、详情和编辑弹窗并保存 | 前端不展示、不编辑、不提交个案经理和压疮风险；后端 DTO、实体和迁移不再保留对应字段 | 是 | P0 |

## 4. 执行记录

| 时间 | 环境 | 命令/方式 | 结果 | 失败摘要 | 修复记录 |
| --- | --- | --- | --- | --- | --- |
| 2026-04-26 | local | `npm test` | Pass | 首次失败：JSDOM 缺少 `matchMedia`，角色下拉测试命中多个 combobox | 已补浏览器 API stub，并收窄测试选择器后复跑通过，7/7 tests passed |
| 2026-04-26 | local | `npm run build` | Pass | 首次失败：`vite.config.ts` 中 test 字段类型不被生产构建识别 | 拆分 `vitest.config.ts` 后复跑通过；仍有 Ant Design chunk size warning |
| 2026-04-26 | local | `npm test` | Pass | monorepo 迁移后复跑 | `packages/shared-domain` 5/5，`apps/web-admin` 2/2，通过 |
| 2026-04-26 | local | `npm run build` | Pass | monorepo 迁移后首次失败：shared project reference 不能禁用 emit，缺少 Node 类型 | 共享包改为只产出声明文件，补 `@types/node` 并调整 workspace 顺序后复跑通过；仍有 Ant Design chunk size warning |
| 2026-04-26 | local | `npm test` | Pass | 扩展长者档案字段、健康风险、完整度后复跑 | `packages/shared-domain` 6/6，`apps/web-admin` 2/2，通过 |
| 2026-04-26 | local | `npm run build` | Pass | 扩展长者档案字段、健康风险、完整度后复跑 | 构建通过；仍有 Ant Design chunk size warning |
| 2026-04-26 | local | `npm run test:web` | Pass | 新建档案从前端草稿改为调用后端 `POST /api/v1/residents` 后复跑 | Web 管理端 4/4 通过；仍有 Ant Design act/message 测试警告 |
| 2026-04-26 | local | `npm run build:web` | Pass | 新建档案后端 API 接入后复跑 | 构建通过；仍有 Ant Design chunk size warning |
| 2026-04-27 | local | `npm run test:web` | Pass | 列表、详情和创建完整接入后端 API 后复跑 | Web 管理端 5/5 通过；仍有 Ant Design act/message 测试警告 |
| 2026-04-27 | local | `npm run build:web` | Pass | 列表、详情和创建完整接入后端 API 后复跑 | 构建通过；仍有 Ant Design chunk size warning |
| 2026-04-27 | local | `npm run test:web` | Pass | 新建档案格式校验、出生日期选择器、列表加载失败可见错误后复跑 | Web 管理端 7/7 通过；仍有 Ant Design act/message 测试警告 |
| 2026-04-27 | local | `npm run build:web` | Pass | 新建档案格式校验、出生日期选择器、列表加载失败可见错误后复跑 | 构建通过；仍有 Ant Design chunk size warning |
| 2026-04-27 | local | `npm run test:web` | Pass | 默认亮色主题、登录页亮色主题、查看档案编辑 PATCH、删除前端合成详情时间线后复跑 | Web 管理端 9/9 通过；首次失败为测试替身仍返回脱敏证件号，已改为敏感角色详情明文契约后复跑通过 |
| 2026-04-27 | local | `npm run build:web` | Pass | 查看档案编辑、亮暗主题和详情模拟数据清理后复跑 | 构建通过；仍有 Ant Design chunk size warning |
| 2026-04-27 | local | `npm run test:web` | Pass | 编辑弹窗分页、时间线格式和操作人名称、待补字段用户字段名后复跑 | Web 管理端 9/9 通过；首次失败为创建用例超时和抽屉/弹窗同名 Tab 查询歧义，已限定编辑弹窗查询并放宽慢用例超时后复跑通过；仍有 Ant Design act/message 测试警告 |
| 2026-04-27 | local | `npm run build:web` | Pass | 编辑弹窗分页和详情展示优化后复跑 | 构建通过；仍有 Ant Design chunk size warning |
| 2026-04-27 | local | `cd apps/backend && mvn test -Dtest=ResidentProfileControllerTest` | Pass | 详情响应补充创建/更新人展示名称后复跑 | 后端长者档案控制器测试 9/9 通过；首次失败为测试 token userId 与 seed 用户 id 不一致，已优先使用当前认证上下文 displayName 后复跑通过 |
| 2026-04-27 | local | `npm run test:web` | Pass | 新增/编辑档案分页按钮改为前几页下一页、最后保存，并移除房间床位格式校验后复跑 | Web 管理端 9/9 通过；仍有 Ant Design act/message 测试警告 |
| 2026-04-27 | local | `npm run build:web` | Pass | 分页按钮和房间校验调整后复跑 | 构建通过；仍有 Ant Design chunk size warning |
| 2026-04-27 | local | `npm run test:web` | Pass | 身份证自动推导出生日期/年龄、补齐责任社工编辑入口后复跑 | Web 管理端 9/9 通过；仍有 Ant Design act/message 测试警告 |
| 2026-04-27 | local | `npm run build:web` | Pass | 身份证自动推导和可编辑字段补齐后复跑 | 构建通过；仍有 Ant Design chunk size warning |
| 2026-04-27 | local | `cd apps/backend && mvn test -Dtest=ResidentProfileControllerTest` | Pass | 修复 PostgreSQL 下列表查询 `lower(bytea)` 导致档案加载失败，并补按房间关键字和无关键字列表回归 | 后端长者档案控制器测试 9/9 通过；新增用例首轮因测试数据共用 301 房间命中多条，改为唯一 901 房间后通过 |
| 2026-04-27 | local | `npm run test:shared` | Pass | 删除共享领域模型中的年龄、个案经理和压疮风险字段后复跑 | 共享包 10/10 通过 |
| 2026-04-27 | local | `npm run test:web` | Pass | 年龄改为按出生日期实时计算，前后端删除个案经理/压疮风险，编辑弹窗 z-index 提升后复跑 | Web 管理端 9/9 通过；首轮失败为只读年龄控件缺少可访问标签，已补 `aria-label` 后通过；仍有 Ant Design act/message 测试警告 |
| 2026-04-27 | local | `npm run build:web` | Pass | 实时年龄、字段删除和弹窗层级调整后复跑 | 构建通过；仍有 Ant Design chunk size warning |
| 2026-04-27 | local | `cd apps/backend && mvn test -Dtest=ResidentProfileControllerTest,RoomControllerTest` | Pass | 后端 DTO、实体和 Flyway 删除个案经理/压疮风险字段后复跑 | 后端长者档案和房间相关控制器测试 18/18 通过；存在 Spring Security generated password、Flyway H2 版本、JDK native access 非阻断警告 |
| 2026-04-26 | local | `npm test` | Pass | 前端重设计后补充主题切换测试并复跑 | `packages/shared-domain` 6/6，`apps/web-admin` 3/3，通过；仍有 Ant Design act warning |
| 2026-04-26 | local | `npm run build` | Pass | 前端重设计后复跑 | 构建通过；仍有 Ant Design chunk size warning |

## 5. 回归结论

- 已通过：权限/脱敏/机构过滤单测，Web 工作台渲染、物业主管只读入口、后端列表/详情加载、创建 API 调用、创建表单格式校验、列表加载失败可见错误、生产构建。
- 未通过：无。
- 跳过项及原因：真实导出任务尚未实现，等待导出审批、文件生成和异步任务模块。
- 剩余风险：Web 管理端首包仍需后续代码分割优化；端到端浏览器自动化尚未覆盖真实后端联调环境。

## 8. 后端接口接入记录

| 日期 | 环境 | 命令 | 结果 | 覆盖内容 | 备注 |
| --- | --- | --- | --- | --- | --- |
| 2026-04-26 | local | `cd apps/backend && mvn test` | Pass | 长者档案创建、列表、详情、敏感字段脱敏/明文权限、重复档案防重、幂等创建、角色越权、乐观锁修改、作废、导出未实现显式 501、房间床位分配引用真实档案 | 后端 16/16 通过；导出任务仍等待审批/异步任务模块 |
| 2026-04-27 | local | `cd apps/backend && mvn test -Dtest=ResidentProfileControllerTest` | Pass | 档案列表按姓名、档案号、房间、位置搜索；无关键字列表加载；回归 PostgreSQL `lower(bytea)` 查询故障 | 后端长者档案控制器测试 9/9 通过 |
