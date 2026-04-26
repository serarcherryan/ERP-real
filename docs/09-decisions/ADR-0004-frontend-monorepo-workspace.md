# ADR-0004: Frontend Monorepo Workspace

| 字段 | 内容 |
| --- | --- |
| 状态 | Accepted |
| 日期 | 2026-04-26 |
| 决策人 | TBD |
| 影响范围 | Web 管理端、小程序端、后续 App、共享前端包、工程脚本 |

## Context

系统需要同时支持 Web 管理端、员工小程序、家属/用户小程序和后续 App。多端会共享长者档案、工单、权限、字段脱敏、字典、OpenAPI DTO 和测试用例。如果每个端独立建仓或在根目录平铺源码，容易出现接口语义漂移、权限规则重复实现和测试命令不可追踪。

## Decision

采用 npm workspaces 管理前端 monorepo：

```text
apps/
  web-admin/
  staff-miniapp/
  family-miniapp/
  mobile-app/
packages/
  shared-domain/
  api-client/
  ui-tokens/
```

- `apps/` 存放具体端侧应用。
- `packages/` 存放跨端共享能力。
- 根目录 `package.json` 负责统一 `test`、`build` 和端侧启动脚本。
- 当前 Web 管理端迁移到 `apps/web-admin`。
- 当前角色、权限、长者档案 mock、字段脱敏和共享业务规则迁移到 `packages/shared-domain`。

## Options Considered

| 方案 | 优点 | 缺点 | 结论 |
| --- | --- | --- | --- |
| 单仓库 npm workspaces | 契约、共享类型、测试命令和 PR 管理统一 | 需要维护 workspace 边界和依赖方向 | 采纳 |
| 多仓库分别管理 | 发布隔离强，单端仓库更小 | 契约和权限规则容易漂移，跨端变更协调成本高 | 不采纳 |
| 根目录平铺多个端 | 初期简单 | 目录边界不清，构建和测试脚本难治理 | 不采纳 |

## Consequences

- 正面影响：多端共享类型和权限规则，后续小程序/App 可复用 `packages/shared-domain` 和 OpenAPI client。
- 负面影响：workspace 构建顺序、包导出和依赖方向需要持续治理。
- 成本：需要维护根脚本、tsconfig 引用、共享包版本和端侧构建配置。
- 约束：共享包不得依赖具体端；端侧不得复制共享权限和脱敏规则。

## Validation

- `npm test` 应覆盖所有 workspace 测试。
- `npm run build` 应按 workspace 构建共享包和端侧应用。
- 新增端侧应用时，应能通过根目录脚本单独启动和测试。
- 代码评审检查 `apps` 与 `packages` 的依赖方向。
