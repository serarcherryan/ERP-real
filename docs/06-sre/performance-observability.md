# Performance And Observability

## 1. 容量模型模板

| 指标 | 当前预估 | 1 年 | 3 年 | 说明 |
| --- | --- | --- | --- | --- |
| 租户数 |  |  |  |  |
| 机构数 |  |  |  |  |
| 老人数 |  |  |  |  |
| 员工数 |  |  |  |  |
| 家属用户数 |  |  |  |  |
| 日活用户 |  |  |  |  |
| 峰值 QPS |  |  |  |  |
| 日新增护理记录 |  |  |  |  |
| 日通知量 |  |  |  |  |
| 报表导出量 |  |  |  |  |

## 2. 性能分级

| 等级 | 场景 | 响应目标 | 容错策略 |
| --- | --- | --- | --- |
| P0 | 护理记录、告警、支付 | P95 <= 300ms | 降级、重试、人工补偿 |
| P1 | 档案、排班、库存 | P95 <= 500ms | 缓存、异步化 |
| P2 | 报表、导出、分析 | 异步完成 | 任务队列、通知结果 |

## 3. 必备指标

- HTTP: QPS、P50/P95/P99、错误率、状态码分布
- DB: 慢查询、连接池、锁等待、事务时长
- Cache: 命中率、热 key、穿透、雪崩
- Queue: 积压、消费延迟、死信、重试
- Business: 入住办理数、护理任务完成率、告警处理时长、账单成功率
- Tenant: 按租户和机构维度拆分核心指标

## 4. 日志字段规范

```json
{
  "timestamp": "2026-04-25T00:00:00Z",
  "level": "INFO",
  "traceId": "trace-id",
  "tenantId": "tenant-id",
  "facilityId": "facility-id",
  "userId": "user-id",
  "role": "staff",
  "module": "care-task",
  "action": "complete_task",
  "resourceId": "resource-id",
  "message": "business event message"
}
```

## 5. 告警基线

| 告警 | 触发条件 | 处理人 | 响应 |
| --- | --- | --- | --- |
| P0 API 错误率 | 5 分钟 > 1% | On-call | 立即 |
| P0 API P95 | 5 分钟 > 1s | On-call | 立即 |
| 队列积压 | 超过消费能力 10 分钟 | Backend/SRE | 15 分钟 |
| 死信增长 | 连续增长 | Backend | 30 分钟 |
| DB 慢查询 | P95 超阈值 | Backend/DBA | 30 分钟 |
| 租户越权 | 任意发生 | Security | 立即 |

## 6. 压测报告模板

```text
目标:
版本:
环境:
数据规模:
场景:
并发用户:
QPS:
P95/P99:
错误率:
资源使用:
瓶颈:
优化项:
是否通过:
```

