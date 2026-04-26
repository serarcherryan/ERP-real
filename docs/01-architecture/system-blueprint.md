# System Blueprint

## 1. 架构目标

本系统面向养老中心、物业条线、用户/家属和管理层，首期优先交付长者档案管理、工单管理和数据驾驶舱。架构目标是在快速交付的同时，保留后续高并发、多端、多角色、多机构扩展能力。

| 目标 | 说明 | 验证方式 |
| --- | --- | --- |
| 多端一致 | Web 端、小程序端、驾驶舱共享业务契约 | OpenAPI/事件契约、契约测试 |
| 模块边界清晰 | 前期采用模块化单体，业务边界按领域划分 | 模块文档、依赖检查、代码包边界 |
| 高并发可演进 | 核心交易、统计分析、通知推送、文件导出解耦 | 压测、队列积压、慢查询、容量模型 |
| 多租户隔离 | 所有业务数据默认按租户、机构隔离 | 租户隔离测试、权限测试 |
| 可追溯 | 长者档案、健康、工单、导出等关键行为全链路审计 | 审计日志抽样、合规检查 |
| 可观测 | 故障可以定位，容量可以预测 | 指标、日志、链路追踪、告警 |

## 2. 推荐技术栈

| 层 | 技术选型 | 说明 |
| --- | --- | --- |
| Web 管理端 | React + TypeScript + Ant Design Pro | 适合 ERP 后台、表格、表单、权限菜单和多视图 |
| 小程序端 | Taro + React + TypeScript | 复用前端技术栈，优先支持微信小程序 |
| 后端 | Java 21 + Spring Boot 3 + Spring Modulith | 前期模块化单体，后期按压力拆服务 |
| API 契约 | REST + OpenAPI | Web、小程序、内部服务统一契约 |
| 主数据库 | PostgreSQL | 事务、复杂查询、JSONB、分区和索引能力较强 |
| 缓存 | Redis | 权限缓存、字典缓存、热点数据、幂等键、限流 |
| 消息队列 | Kafka 或 RabbitMQ | 工单状态、通知、审计、数据驾驶舱异步同步 |
| 搜索 | OpenSearch / Elasticsearch | 长者档案、工单、公告、日志全文检索 |
| 文件存储 | MinIO / S3 兼容对象存储 | 合同、报告、照片、附件 |
| 数据分析 | ClickHouse | 驾驶舱、报表、服务频次、趋势分析 |
| 工作流 | Flowable / Camunda 轻量使用 | 入住审批、换房审批、工单验收、投诉闭环 |
| 可观测性 | OpenTelemetry + Prometheus + Grafana + Loki | 链路追踪、指标、日志、告警 |

## 3. 逻辑架构

```text
Clients
  Web Admin / Staff Mini Program / User Mini Program / Dashboard
        |
API Gateway / BFF Layer
  Auth, rate limit, request aggregation, view-specific permission trimming
        |
Modular Backend Application
  Tenant & Organization
  Auth & Permission
  Resident Profile
  Admission & Living
  Work Order
  Care & Health
  Activity & Publishing
  Inventory
  Notification
  Dashboard
  Audit & Compliance
        |
Shared Capabilities
  Security, tenancy, workflow, idempotency, file, observability
        |
Data Stores & Middleware
  PostgreSQL / Redis / MQ / OpenSearch / MinIO / ClickHouse
```

## 4. 端与视图

| 端/视图 | 主要角色 | 核心能力 |
| --- | --- | --- |
| 物业管理 Web 视图 | 物业经理、物业主管 | 房态、维修、保洁、派工、验收、物资 |
| 养老中心 Web 视图 | 社工、社工主管、部门经理 | 长者档案、关怀沟通、活动、待办、审核 |
| 运营驾驶舱 | 部门经理、管理层 | 入住率、服务频次、工单趋势、异常与绩效 |
| 系统管理 Web 视图 | 超管、机构管理员 | 租户、机构、部门、角色、权限、字典 |
| 员工小程序 | 社工、物业人员、护理人员 | 待办、工单处理、巡访记录、拍照上传 |
| 用户/家属小程序 | 用户、家属 | 服务预约、通知查看、活动报名、沟通记录 |

## 4.1 前端 Monorepo 目录

```text
apps/
  web-admin/          Web 管理端
  staff-miniapp/      员工小程序，后续按 Taro 创建
  family-miniapp/     家属/用户小程序，后续按 Taro 创建
  mobile-app/         后续 App
packages/
  shared-domain/      跨端领域类型、角色权限、字段脱敏和共享业务规则
  api-client/         OpenAPI 生成的请求客户端和 DTO
  ui-tokens/          设计变量、主题和跨端样式 token
```

- 端侧应用只放在 `apps/`，共享能力只放在 `packages/`。
- `packages/shared-domain` 可被 Web、小程序和 App 共同依赖，但不得反向依赖具体端。
- API DTO、权限常量、字段策略和字典应优先共享，避免多端重复定义。
- 根目录 workspace 脚本负责统一测试、构建和端侧启动。

## 5. 首期交付边界

| 模块 | 范围 | 不做范围 |
| --- | --- | --- |
| 长者档案管理 | 基础信息、入住状态、房间床位、家属联系人、标签、健康摘要、服务记录时间线 | 完整医疗病历、完整财务账单 |
| 工单管理 | 创建、分派、接单、执行、验收、关闭、退回、超时提醒、回访评价 | 外部维修商结算、复杂 SLA 计费 |
| 数据驾驶舱 | 入住率、房态、年龄/性别/疾病分类、工单数量、服务频次、待办超时、活动参与率 | 自助 BI、任意 SQL 查询 |

## 6. 架构边界

- BFF 负责端适配、聚合查询和展示层权限裁剪，不承载核心业务规则。
- Application Service 负责编排流程，核心不变量放在所属模块的领域服务或领域对象。
- 跨模块写入必须经过 owning module 的应用服务，禁止绕过模块直接修改其他模块表。
- 跨模块同步调用必须有超时、降级、幂等和错误处理。
- 跨模块异步事件必须定义事件名、版本、幂等键、重放策略和失败补偿。
- 数据驾驶舱只读分析数据，不反向写入核心业务表。
- 报表导出、通知推送、搜索索引、驾驶舱统计必须从在线请求链路解耦。

## 7. 高并发设计策略

| 场景 | 风险 | 设计策略 |
| --- | --- | --- |
| 驾驶舱高频刷新 | 聚合查询压垮业务库 | ClickHouse、预聚合、指标快照、缓存 |
| 工单集中提交/派单 | 重复提交、状态乱跳、热点更新 | 幂等键、乐观锁、状态机非法流转校验、MQ 削峰 |
| 长者档案一人一档 | 超级聚合接口变慢 | 摘要投影、分 Tab 懒加载、BFF 聚合、只读投影 |
| 权限加载 | 多角色、多机构、多字段权限查询变重 | Redis 缓存、权限版本号、变更失效 |
| 消息提醒风暴 | 推送链路拥塞 | 独立通知队列、限流、合并、优先级、死信队列 |
| 文件上传和报表导出 | 大文件和长任务阻塞在线请求 | 对象存储、异步任务、临时下载链接、导出审计 |
| 搜索与模糊查询 | OLTP 模糊查询拖慢核心交易 | OpenSearch 索引、异步同步、降级到精确查询 |

## 8. 数据架构

```text
PostgreSQL
  核心交易数据、强一致写入、状态机、权限与审计主记录

MQ / CDC
  工单状态、长者档案变更、入住状态、服务记录、审计事件异步同步

ClickHouse
  驾驶舱指标、趋势分析、聚合宽表、报表查询

OpenSearch
  档案、工单、公告、日志全文检索

MinIO / S3
  合同、体检报告、图片、附件和导出文件
```

典型事件链路：

```text
工单完成
  -> 写入 PostgreSQL
  -> 发布 work_order.status_changed.v1
  -> 审计模块记录关键操作
  -> 通知模块推送提醒
  -> 数据中心消费事件更新指标
```

## 9. 部署架构

前期采用容器化模块化单体部署，保留服务拆分边界：

```text
Nginx / Ingress
  -> Web Static Assets
  -> API Gateway / BFF
      -> Backend App
      -> Worker App
      -> Scheduler App
PostgreSQL / Redis / MQ / OpenSearch / MinIO / ClickHouse
Prometheus / Grafana / Loki / Tempo
```

优先拆分顺序：

1. Notification Service
2. Dashboard Service
3. Work Order Service
4. File Service

## 10. 非功能需求基线

| 指标 | P0 核心流程 | P1 重要流程 | P2 普通流程 |
| --- | --- | --- | --- |
| 可用性 | 99.95% | 99.9% | 99.5% |
| P95 响应 | <= 300ms | <= 500ms | <= 1000ms |
| 错误率 | <= 0.1% | <= 0.3% | <= 1% |
| RPO | <= 5min | <= 30min | <= 24h |
| RTO | <= 30min | <= 2h | <= 24h |
| 审计 | 全量 | 关键行为 | 按需 |

## 11. 技术决策记录

已固化的技术决策：

- `docs/09-decisions/ADR-0001-platform-architecture.md`
- `docs/09-decisions/ADR-0002-frontend-stack.md`
- `docs/09-decisions/ADR-0003-data-analytics-architecture.md`
- `docs/09-decisions/ADR-0004-frontend-monorepo-workspace.md`

任何满足以下条件的选择都必须继续创建 ADR：

- 改变系统边界或数据所有权
- 引入新的基础设施、中间件或核心框架
- 改变认证、权限、审计、隐私策略
- 对性能、成本或发布流程有长期影响
