# Work Order Module

## 1. 模块档案

| 字段 | 内容 |
| --- | --- |
| 模块名称 | 工单管理 |
| 所属业务域 | 工单中心 |
| 负责人 | TBD |
| 业务优先级 | P0 |
| 首发端 | Web 管理端 / 员工小程序 |
| 依赖模块 | 机构与租户、用户与权限、长者档案、消息通知、审计合规、文件服务 |
| 下游影响 | 数据中心、关怀沟通、服务质量、物资库存 |

## 2. 业务目标

- 解决的问题：统一保洁、维修、服务派工、执行、验收和回访闭环。
- 目标用户：物业经理、物业主管、社工、社工主管、部门经理、执行人员。
- 成功指标：工单闭环率、平均响应时长、超时率、重复提交率、验收通过率。
- 不做范围：外部供应商结算、复杂 SLA 计费、跨系统派单。

## 3. 核心流程

```text
提交工单 -> 分派工单 -> 接单 -> 执行 -> 完成 -> 验收 -> 关闭
                       -> 退回/驳回 -> 重新处理
```

## 4. 领域模型

| 实体/聚合 | 说明 | 所有者 | 生命周期 |
| --- | --- | --- | --- |
| WorkOrder | 工单聚合根 | 工单中心 | Draft / Submitted / Assigned / InProgress / Completed / Accepted / Closed / Cancelled |
| Assignment | 分派记录 | 工单中心 | Active / Reassigned |
| WorkOrderAction | 接单、处理、退回、完成等动作记录 | 工单中心 | Immutable |
| Acceptance | 验收记录 | 工单中心 | Pending / Accepted / Rejected |
| WorkOrderAttachment | 现场照片、处理凭证等附件引用 | 工单中心 | Active / Deleted |

## 5. 数据模型草案

| 表/集合 | 用途 | 关键字段 | 索引 | 数据量预估 |
| --- | --- | --- | --- | --- |
| work_orders | 工单主表 | id, tenant_id, facility_id, type, priority, status, resident_id, location, idempotency_key, version | tenant_id + facility_id + status, tenant_id + assignee_id + status, idempotency_key | 每机构日百级到千级 |
| work_order_assignments | 分派记录 | id, work_order_id, assignee_id, assigned_by, assigned_at | work_order_id, assignee_id + assigned_at | 与工单动作同级 |
| work_order_actions | 工单动作流水 | id, work_order_id, action, from_status, to_status, operator_id, reason | work_order_id + created_at | 工单数 3-10 倍 |
| work_order_acceptances | 验收记录 | id, work_order_id, result, reviewer_id, reason | work_order_id | 与需验收工单同级 |
| work_order_attachments | 附件引用 | id, work_order_id, file_id, category | work_order_id + category | 按现场照片增长 |

## 6. API 与事件

| 类型 | 名称 | 调用方/订阅方 | 契约文件 |
| --- | --- | --- | --- |
| API | Work Order Create / Assign / Transition | Web 管理端、员工小程序 | 待创建 |
| API | Work Order List / Detail | Web 管理端、员工小程序、驾驶舱 BFF | 待创建 |
| Event | work_order.status_changed.v1 | 数据中心、通知、审计、搜索 | `docs/03-apis/work-order-status-changed-event.md` |

## 7. 权限与数据范围

| 操作 | 角色 | 数据范围 | 审计 |
| --- | --- | --- | --- |
| 创建 | 社工、物业主管、用户/家属授权入口 | 所属机构或绑定长者 | 是 |
| 查看 | 社工、社工主管、物业主管、物业经理、部门经理 | 授权机构、部门、本人相关工单 | 关键工单是 |
| 修改基础信息 | 创建人、主管角色 | 未进入执行前，授权范围内 | 是 |
| 分派 | 物业主管、社工主管、部门经理 | 所属机构、部门 | 是 |
| 接单/执行/完成 | 被分派执行人 | 本人待办 | 是 |
| 验收/关闭 | 主管角色或发起方授权角色 | 授权范围内 | 是 |
| 取消/作废 | 主管角色 | 授权范围内，需原因 | 是 |

## 8. 并发与一致性

- 是否存在重复提交：存在，创建和状态流转必须支持幂等键。
- 是否需要乐观锁：需要，状态流转使用 `version` 防并发覆盖。
- 是否需要分布式锁：默认不需要，热点工单可按 `work_order_id` 评估短锁。
- 是否允许最终一致：通知、搜索索引、驾驶舱统计允许最终一致。
- 失败补偿方式：状态事件重试、通知补偿、驾驶舱重算、死信处理。

## 9. 可观测性

| 类型 | 内容 |
| --- | --- |
| 日志 | workOrderId、tenantId、facilityId、operatorId、fromStatus、toStatus、traceId |
| 指标 | 创建量、状态流转量、超时量、关闭率、P95、并发冲突数、幂等命中数 |
| 告警 | 状态流转失败突增、通知队列积压、超时工单突增、死信数量 |
| 审计 | 创建、分派、接单、完成、验收、退回、关闭、作废 |

## 10. 测试计划

- 单元测试：状态机合法/非法流转、幂等、乐观锁、权限判断。
- 集成测试：创建到关闭闭环、并发状态流转、事件发布、审计写入。
- 契约测试：工单创建、分派、状态流转、列表分页。
- E2E 测试：物业主管派单、执行人小程序完成、主管验收关闭。
- 性能测试：集中提交、批量派单、员工待办列表。
- 权限/越权测试：跨租户、跨机构、非执行人操作、字段脱敏。
- 数据迁移测试：历史工单导入和状态映射。

## 11. 发布计划

- Feature flag：按工单类型、机构开启。
- 灰度范围：先保洁/维修工单，再扩展服务工单和用户端入口。
- 数据迁移：历史工单导入时保留原始状态和转换映射。
- 回滚方式：关闭新建入口，保留查询和处理存量工单。
- 监控观察窗口：首发后至少 7 天观察闭环率、超时率、并发冲突和通知积压。
