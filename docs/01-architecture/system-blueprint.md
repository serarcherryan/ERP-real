# System Blueprint

## 1. 架构目标

| 目标 | 说明 | 验证方式 |
| --- | --- | --- |
| 多端一致 | 用户端、企业端、员工端共享业务契约 | OpenAPI/事件契约、契约测试 |
| 高并发 | 支撑高峰访问、批量任务、实时告警 | 压测报告、容量模型 |
| 多租户 | 支撑单院、连锁、集团化运营 | 租户隔离测试、权限测试 |
| 可追溯 | 关键业务全链路审计 | 审计日志抽样、合规检查 |
| 可演进 | 模块可独立扩展和替换 | ADR、模块边界检查 |
| 可观测 | 故障可以定位、容量可以预测 | 指标、日志、链路追踪 |

## 2. 建议逻辑架构

```text
Clients
  Web Admin / Staff App / Family App / Enterprise Portal
        |
API Gateway / BFF Layer
        |
Application Services
  Admission
  Resident Profile
  Care Plan
  Nursing Task
  Medical Record
  Medication
  Meal & Nutrition
  Billing
  Inventory
  HR & Scheduling
  Notification
  Reporting
        |
Domain Services / Shared Capabilities
  Auth & RBAC
  Tenant Management
  Workflow
  Audit Log
  File Service
  Message/Event Bus
        |
Data Stores
  OLTP DB / Cache / Search / Object Storage / Analytics Store
```

## 3. 架构边界

- BFF 负责端适配、聚合、展示层权限裁剪，不承载核心业务规则。
- Application Service 编排流程，但核心不变量应下沉到 Domain Service 或领域对象。
- 跨模块同步调用必须有超时、降级和幂等策略。
- 跨模块异步事件必须定义事件名、版本、幂等键、重放策略和失败补偿。
- 报表和分析尽量从业务写路径解耦，避免压垮核心交易库。

## 4. 高并发基础策略

| 场景 | 风险 | 设计策略 |
| --- | --- | --- |
| 家属集中查看账单/动态 | 读高峰 | CDN、缓存、读写分离、分页 |
| 员工交接班集中提交记录 | 写高峰 | 队列削峰、幂等提交、批量写入 |
| 告警实时推送 | 消息风暴 | 优先级队列、限流、合并通知 |
| 报表导出 | 长任务拖垮在线服务 | 异步任务、导出队列、对象存储 |
| 多机构运营看板 | 聚合查询复杂 | 预聚合、数据仓库、物化视图 |

## 5. 非功能需求基线

| 指标 | P0 核心流程 | P1 重要流程 | P2 普通流程 |
| --- | --- | --- | --- |
| 可用性 | 99.95% | 99.9% | 99.5% |
| P95 响应 | <= 300ms | <= 500ms | <= 1000ms |
| 错误率 | <= 0.1% | <= 0.3% | <= 1% |
| RPO | <= 5min | <= 30min | <= 24h |
| RTO | <= 30min | <= 2h | <= 24h |
| 审计 | 全量 | 关键行为 | 按需 |

## 6. 技术决策记录

任何满足以下条件的选择都必须创建 ADR：

- 改变系统边界或数据所有权
- 引入新的基础设施、中间件或核心框架
- 改变认证、权限、审计、隐私策略
- 对性能、成本或发布流程有长期影响

