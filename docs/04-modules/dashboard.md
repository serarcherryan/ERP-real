# Dashboard Module

## 1. 模块档案

| 字段 | 内容 |
| --- | --- |
| 模块名称 | 数据驾驶舱 |
| 所属业务域 | 数据中心 |
| 负责人 | TBD |
| 业务优先级 | P0 |
| 首发端 | Web 管理端 / 运营驾驶舱 |
| 依赖模块 | 长者档案、入住生活、工单中心、活动与发布、审计合规 |
| 下游影响 | 管理决策、运营复盘、服务质量考核 |

## 2. 业务目标

- 解决的问题：为管理层和部门主管提供入住、房态、长者画像、服务频次、工单效率等核心指标。
- 目标用户：部门经理、物业经理、社工主管、运营管理层。
- 成功指标：驾驶舱 P95、指标刷新延迟、指标口径一致性、报表导出成功率。
- 不做范围：自助 BI、任意 SQL 查询、跨系统财务分析。

## 3. 核心流程

```text
业务事件产生 -> 指标消费/同步 -> 预聚合写入分析库 -> 驾驶舱查询 -> 指标下钻/导出 -> 审计记录
```

## 4. 领域模型

| 实体/聚合 | 说明 | 所有者 | 生命周期 |
| --- | --- | --- | --- |
| MetricSnapshot | 指标快照聚合根 | 数据中心 | Pending / Ready / Expired |
| MetricDefinition | 指标定义和口径版本 | 数据中心 | Draft / Active / Deprecated |
| MetricDimension | 指标维度，如机构、部门、日期、工单类型 | 数据中心 | Active |
| ReportExport | 报表导出任务 | 数据中心 | Pending / Running / Succeeded / Failed / Expired |

## 5. 数据模型草案

| 表/集合 | 用途 | 关键字段 | 索引 | 数据量预估 |
| --- | --- | --- | --- | --- |
| dashboard_metric_definitions | 指标定义 | metric_code, version, formula, owner_module | metric_code + version | 百级 |
| dashboard_metric_snapshots | 指标快照 | tenant_id, facility_id, metric_code, dimensions, value, period_start, period_end | tenant_id + facility_id + metric_code + period_start | 随指标和时间增长 |
| dashboard_wide_work_order_daily | 工单日聚合宽表 | tenant_id, facility_id, date, type, status, count, avg_duration | date + facility_id + type | 每日增量 |
| dashboard_wide_resident_daily | 长者日聚合宽表 | tenant_id, facility_id, date, resident_count, occupancy_rate | date + facility_id | 每日增量 |
| report_exports | 导出任务 | id, tenant_id, facility_id, status, file_id, requested_by, expire_at | tenant_id + requested_by + created_at | 按导出量增长 |

## 6. API 与事件

| 类型 | 名称 | 调用方/订阅方 | 契约文件 |
| --- | --- | --- | --- |
| API | Dashboard Metric Query | 运营驾驶舱、Web 管理端 | 待创建 |
| API | Dashboard Export | 运营驾驶舱、Web 管理端 | 待创建 |
| Event | work_order.status_changed.v1 | 数据中心订阅 | `docs/03-apis/work-order-status-changed-event.md` |
| Event | resident_profile.updated.v1 | 数据中心订阅；当前源模块尚未发布 | `docs/03-apis/resident-profile-updated-event.md` |
| Event | dashboard.metric_refreshed.v1 | 通知、审计、运维监控 | 待创建 |

## 7. 权限与数据范围

| 操作 | 角色 | 数据范围 | 审计 |
| --- | --- | --- | --- |
| 查看 | 社工主管、物业经理、部门经理、管理层 | 授权租户、机构、部门 | 敏感指标是 |
| 下钻 | 授权主管和管理层 | 授权范围内，遵循源模块字段权限 | 是 |
| 导出 | 部门经理、授权管理员 | 授权范围内，需原因 | 是 |
| 修改指标口径 | 系统管理员、技术负责人 | 全局配置，需审批 | 是 |

## 8. 并发与一致性

- 是否存在重复提交：导出任务需要幂等，避免重复生成大文件。
- 是否需要乐观锁：指标定义变更需要版本控制。
- 是否需要分布式锁：定时重算同一指标窗口时需要防重复执行。
- 是否允许最终一致：允许，驾驶舱指标默认 T+分钟级或按配置刷新。
- 失败补偿方式：事件重放、指标窗口重算、导出任务重试、死信队列。

## 9. 可观测性

| 类型 | 内容 |
| --- | --- |
| 日志 | metricCode、tenantId、facilityId、period、operatorId、traceId |
| 指标 | 查询 P95、刷新延迟、消费延迟、导出成功率、ClickHouse 慢查询 |
| 告警 | 指标刷新失败、消费积压、导出失败、分析库不可用 |
| 审计 | 敏感指标查看、下钻、导出、指标口径变更 |

## 10. 测试计划

- 单元测试：指标口径计算、维度过滤、权限裁剪。
- 集成测试：事件消费、预聚合写入、查询接口、导出任务；当前等待源模块事件发布和驾驶舱后端实现后补充。
- 契约测试：指标查询、导出创建、导出状态查询。
- E2E 测试：管理层查看驾驶舱、下钻工单指标、导出报表。
- 性能测试：高并发刷新、复杂维度查询、导出队列压力。
- 权限/越权测试：跨租户、跨机构、字段级下钻权限。
- 数据迁移测试：指标重算、口径版本升级。

## 11. 发布计划

- Feature flag：按指标和机构开启。
- 灰度范围：先只读核心指标，再开放下钻和导出。
- 数据迁移：初始化历史指标快照，保留口径版本。
- 回滚方式：回退到上一版本指标定义，关闭问题指标卡片。
- 监控观察窗口：首发后至少 7 天观察刷新延迟、查询 P95 和导出失败率。
