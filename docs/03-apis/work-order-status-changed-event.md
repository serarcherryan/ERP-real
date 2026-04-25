# Work Order Status Changed Event

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 事件名称 | work_order.status_changed.v1 |
| 事件版本 | v1 |
| 发布模块 | 工单中心 |
| 订阅模块 | 数据中心、消息通知、审计合规、搜索 |
| 业务级别 | P0 |
| 是否可重放 | 是 |

## 2. 事件语义

- 何时发布：工单状态成功完成一次合法流转并提交事务后发布。
- 表示的业务事实：某个工单已经从一个状态变更为另一个状态。
- 不应该被订阅方误解为：通知已经送达、驾驶舱指标已经完成刷新、验收一定通过。
- 幂等键：`eventId` 或 `workOrderId + toStatus + changedAt + actionId`。
- 顺序要求：同一 `workOrderId` 内必须按状态变更发生时间顺序消费；跨工单无顺序要求。

## 3. Payload

```json
{
  "eventId": "uuid",
  "eventType": "work_order.status_changed.v1",
  "occurredAt": "2026-04-25T00:00:00Z",
  "tenantId": "tenant-id",
  "facilityId": "facility-id",
  "aggregateId": "work-order-id",
  "payload": {
    "workOrderId": "work-order-id",
    "workOrderNo": "WO202604250001",
    "type": "repair",
    "priority": "normal",
    "fromStatus": "InProgress",
    "toStatus": "Completed",
    "residentId": "resident-id-or-null",
    "departmentId": "department-id-or-null",
    "assigneeId": "user-id-or-null",
    "operatorId": "user-id",
    "actionId": "action-id",
    "reason": "completed by assignee"
  }
}
```

## 4. 消费规则

- 消费方必须按 `eventId` 或业务幂等键去重。
- 消费失败必须进入重试或死信队列。
- 事件 schema 只允许兼容性扩展。
- 发布方不得假设消费方一定实时完成。
- 数据中心消费后更新工单指标宽表和快照。
- 消息通知消费后按角色、数据范围和通知策略生成提醒。

## 5. 监控

| 指标 | 目标 |
| --- | --- |
| 发布成功率 | >= 99.99% |
| 消费延迟 P95 | <= 60s |
| 死信数量 | 0，出现即告警 |
| 重试次数 | 连续 3 次失败进入死信 |
