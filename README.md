# 养老院 ERP 管理系统工程模板

这是一个面向多端、高并发、长生命周期业务系统的 Engineering Harness 模板。它的目标不是提前写死技术方案，而是为后续的需求、架构、模块、接口、测试、性能、安全、发布和 AI 协作建立统一的工作方式。

## 适用场景

- 用户端、企业端、管理端、员工端、小程序或移动端等多端协作
- 入住管理、护理计划、健康档案、药品、餐饮、财务、库存、人事、排班、告警、家属沟通等大量业务模块
- 高并发访问、实时告警、审计追踪、权限复杂、数据敏感
- 多团队或 AI Agent 协作开发，需要稳定的工程边界和交付标准

## 推荐目录

```text
docs/
  00-governance/       项目治理、协作规则、需求分层
  01-architecture/     总体架构、上下文、非功能目标
  02-domain/           业务域拆分、聚合、数据边界
  03-apis/             API 契约、事件契约、兼容规则
  04-modules/          模块设计模板
  05-quality/          测试策略、质量门禁、验收清单
  06-sre/              性能、容量、可观测性、故障演练
  07-security/         权限、安全、隐私、合规
  08-delivery/         发布、变更、回滚、环境管理
  09-decisions/        ADR 架构决策记录
  10-ai-collaboration/ AI/工程协作工作流
.github/
  ISSUE_TEMPLATE/      标准化需求、缺陷、技术债入口
  PULL_REQUEST_TEMPLATE.md
```

## 使用顺序

1. 先填写 `docs/00-governance/project-operating-model.md`
2. 再用 `docs/01-architecture/system-blueprint.md` 定义总体边界
3. 每新增一个业务模块，复制 `docs/04-modules/module-template.md`
4. 每新增一个外部接口或跨端接口，复制 `docs/03-apis/api-contract-template.md`
5. 每个重大技术选择都新增一条 ADR
6. 每次上线前走 `docs/08-delivery/release-change-management.md` 的清单

## 让 AI 自动执行 Harness 流程

仓库根目录的 `AGENTS.md` 是 AI Agent 的入口规则。以后让 AI 写代码时，可以直接在任务里加一句：

```text
请按照 AGENTS.md 和 docs/10-ai-collaboration/auto-harness-protocol.md 执行。
```

AI 应自动完成：

- 判断影响业务域和模块
- 创建或更新模块设计文档
- 创建或更新 API/事件契约
- 必要时创建 ADR
- 实现代码
- 补测试或说明测试缺口
- 检查权限、审计、租户隔离、性能、发布与回滚影响

## 核心原则

- 业务边界优先于代码分层
- 接口契约先于实现细节
- 高并发能力用容量模型和压测数据证明
- 权限、审计、隐私从第一天进入设计
- 每个模块都必须有负责人、数据边界、接口边界、测试边界和告警边界
