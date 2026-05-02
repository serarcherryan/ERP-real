# Resident Profile Updated Event

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 事件名称 | resident_profile.updated.v1 |
| 事件版本 | v1 |
| 发布模块 | 长者档案 |
| 订阅模块 | 数据中心、搜索、审计合规、消息通知 |
| 业务级别 | P0 |
| 是否可重放 | 是 |
| 当前实现状态 | 契约已定义，后端尚未发布该事件 |

## 2. 事件语义

- 何时发布：长者档案基础信息、状态、标签、联系人摘要或健康摘要成功变更并提交事务后发布。
- 当前状态：`apps/backend` 仅同步写库并写审计日志，尚未接入事件总线、Outbox 或重试/死信机制；接入前不得假设订阅方会收到该事件。
- 表示的业务事实：长者档案已经发生一次可追溯变更。
- 不应该被订阅方误解为：所有跨模块详情都已同步，或健康/医疗记录已经变更。
- 幂等键：`eventId` 或 `residentId + profileVersion`。
- 顺序要求：同一 `residentId` 内按 `profileVersion` 顺序消费；跨长者无顺序要求。

## 3. Payload

```json
{
  "eventId": "uuid",
  "eventType": "resident_profile.updated.v1",
  "occurredAt": "2026-04-25T00:00:00Z",
  "tenantId": "tenant-id",
  "facilityId": "facility-id",
  "aggregateId": "resident-id",
  "payload": {
    "residentId": "resident-id",
    "residentNo": "R202604250001",
    "profileVersion": 12,
    "status": "Active",
    "changedFields": [
      "familyContacts",
      "tags"
    ],
    "operatorId": "user-id",
    "sourceClient": "web-admin"
  }
}
```

## 4. 消费规则

- 消费方必须按 `eventId` 或 `residentId + profileVersion` 去重。
- 消费失败必须进入重试或死信队列。
- 事件 schema 只允许兼容性扩展。
- 事件不得携带身份证号、手机号、详细病情等明文高敏感字段。
- 搜索模块消费事件后按权限可见字段刷新索引。
- 数据中心消费事件后刷新长者画像和入住相关指标。

## 5. 监控

| 指标 | 目标 |
| --- | --- |
| 发布成功率 | >= 99.99% |
| 消费延迟 P95 | <= 120s |
| 死信数量 | 0，出现即告警 |
| 重试次数 | 连续 3 次失败进入死信 |
