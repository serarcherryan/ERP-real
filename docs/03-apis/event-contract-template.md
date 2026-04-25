# Event Contract Template

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 事件名称 |  |
| 事件版本 | v1 |
| 发布模块 |  |
| 订阅模块 |  |
| 业务级别 | P0 / P1 / P2 |
| 是否可重放 | 是 / 否 |

## 2. 事件语义

- 何时发布：
- 表示的业务事实：
- 不应该被订阅方误解为：
- 幂等键：
- 顺序要求：

## 3. Payload

```json
{
  "eventId": "uuid",
  "eventType": "module.event_name.v1",
  "occurredAt": "2026-04-25T00:00:00Z",
  "tenantId": "tenant-id",
  "facilityId": "facility-id",
  "aggregateId": "aggregate-id",
  "payload": {}
}
```

## 4. 消费规则

- 消费方必须按 `eventId` 或业务幂等键去重。
- 消费失败必须进入重试或死信队列。
- 事件 schema 只允许兼容性扩展。
- 发布方不得假设消费方一定实时完成。

## 5. 监控

| 指标 | 目标 |
| --- | --- |
| 发布成功率 |  |
| 消费延迟 P95 |  |
| 死信数量 |  |
| 重试次数 |  |

