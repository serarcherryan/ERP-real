# Testing Harness

## 0. 测试闭环

测试用例的生成、执行、检查、失败修复和回归复跑必须遵循：

```text
docs/05-quality/test-case-lifecycle.md
```

任何功能、缺陷修复、接口、事件、权限、状态机、数据迁移或高并发变更，都必须先根据需求、模块设计和契约生成测试用例，再实现或更新自动化测试，并在最终交付中报告执行结果。

## 1. 测试金字塔

| 层级 | 目标 | 覆盖重点 |
| --- | --- | --- |
| Unit | 快速验证核心规则 | 状态机、金额、权限判断、领域不变量 |
| Integration | 验证模块协作 | DB、缓存、队列、第三方接口 |
| Contract | 锁定跨端/跨服务契约 | OpenAPI、事件 schema、错误码 |
| E2E | 验证关键业务闭环 | 入住、护理执行、收费、告警 |
| Performance | 验证容量和退化行为 | 高并发读写、批量任务、报表导出 |
| Security | 验证权限和隐私 | 越权、租户隔离、审计、脱敏 |

## 2. P0 流程必须覆盖

- 登录与权限加载
- 入住办理
- 老人档案查看和修改
- 护理计划生成
- 护理任务执行
- 生命体征记录
- 药品发放与复核
- 账单生成与支付状态同步
- 告警触发与处理
- 多租户数据隔离

## 3. 质量门禁

| 变更类型 | 必须验证 |
| --- | --- |
| 领域规则 | Unit + Integration |
| API 契约 | Contract + API 测试 |
| 页面流程 | E2E 或关键路径手工验收 |
| 权限变更 | 越权测试 + 审计测试 |
| 数据迁移 | 迁移前后校验 + 回滚演练 |
| 高并发路径 | 压测 + 指标观察 |
| 支付/财务 | 幂等、对账、异常补偿 |

## 3.2 Monorepo 测试命令

| 命令 | 覆盖范围 |
| --- | --- |
| `npm test` | 所有 workspace 中声明的测试 |
| `npm run build` | 所有 workspace 中声明的构建 |
| `npm run test:scripts` | 启动脚本 dry-run 回归，验证端口透传、Docker Compose 命令和后端/Web 启动命令 |
| `npm run test:web` | Web 管理端测试 |
| `npm run test:shared` | 共享领域包测试 |
| `npm run test:backend` | 后端 Maven 测试 |
| `npm run build:backend` | 后端 Maven 打包，包含测试 |

新增 `apps/` 或 `packages/` workspace 时，必须确保根目录命令能覆盖对应测试和构建。

## 3.1 测试用例门禁

| 检查项 | 要求 |
| --- | --- |
| 用例生成 | 从验收标准、模块文档、API/事件契约、权限和审计要求生成 |
| 用例记录 | P0/P1 或跨模块变更必须写入 `docs/05-quality/test-cases/` |
| 自动化优先 | 状态机、权限、租户隔离、幂等、金额、审计必须优先自动化 |
| 执行记录 | 必须记录命令、环境、结果、失败摘要 |
| 失败修复 | 失败后必须分类处理，修复代码或修正用例后复跑 |
| 跳过说明 | 无法执行或无需执行的测试必须说明原因和剩余风险 |

## 4. 测试数据原则

- 测试数据必须包含多租户、多机构、多角色。
- 高敏感样例数据必须脱敏或合成。
- 每个 P0 测试场景要有稳定 fixture。
- 压测数据与功能测试数据隔离。
- 关键状态机要覆盖非法状态跳转。

## 4.1 测试用例目录

复杂变更的测试用例文档放在：

```text
docs/05-quality/test-cases/<module>-<feature>-test-cases.md
```

示例：

```text
docs/05-quality/test-cases/resident-profile-create-test-cases.md
docs/05-quality/test-cases/work-order-transition-test-cases.md
docs/05-quality/test-cases/dashboard-metric-query-test-cases.md
```

## 5. Bug 模板

```text
标题:
环境:
影响模块:
影响角色:
复现步骤:
实际结果:
期望结果:
影响范围:
日志/traceId:
截图/录屏:
严重级别:
是否阻塞发布:
```
