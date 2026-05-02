# Room Management API Contract

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| API 名称 | Room Management |
| 所属模块 | 房间管理 |
| 负责人 | TBD |
| 版本 | v1 |
| 调用端 | Web 管理端 / BFF / 内部服务 |
| 风险等级 | P0 |
| 数据敏感级别 | 敏感 |

## 2. Endpoint

```http
GET /api/v1/rooms/tree
GET /api/v1/rooms
GET /api/v1/rooms/{roomId}
POST /api/v1/rooms
PATCH /api/v1/rooms/{roomId}
POST /api/v1/rooms/{roomId}/disable
POST /api/v1/rooms/{roomId}/assignments
GET /api/v1/rooms/available
```

## 3. 业务说明

- 用户故事：后端统一维护住房空间层级，长者档案和入住办理通过 roomId 引用房间，并展示“区-栋-楼-房”完整位置。
- 前置条件：用户已认证，认证上下文提供 `userId`、`tenantId`、`facilityId`、`role`。
- 后置结果：房间层级被创建或修改；床位分配会在同一事务内更新房间占用数和长者档案 `roomId/bedLabel/livingLocationLabel`。
- 幂等要求：创建房间支持 `Idempotency-Key` 持久化记录，并按 `tenantId + facilityId + floorId + roomNo` 防重复。
- 权限要求：物业经理、物业主管、部门经理可维护；社工、社工主管只读。
- 审计要求：创建、修改、停用、容量变更和床位分配写入 `audit_logs`。
- 当前实现：后端接口已在 `apps/backend` 落地，开发/测试环境用 Bearer token 解析认证上下文；生产身份源后续由独立 ADR 决定。

## 4. Request

### 4.1 GET /api/v1/rooms/tree

```json
{}
```

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| tenantId | string | 是 | 来自认证上下文 | 租户隔离，不接受客户端查询参数覆盖 |
| facilityId | string | 是 | 来自认证上下文 | 机构隔离，不接受客户端查询参数覆盖 |

### 4.2 POST/PATCH /api/v1/rooms

```json
{
  "zoneId": "zone-hecheng-elderly-care",
  "buildingId": "building-1",
  "floorId": "floor-1-3",
  "roomNo": "301",
  "displayName": "301号房",
  "capacity": 2,
  "status": "available",
  "version": 1
}
```

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| zoneId | string | 是 | 首期只能为和成养老区 | 居住区 |
| buildingId | string | 是 | 必须属于 zoneId | 楼栋 |
| floorId | string | 是 | 必须属于 buildingId | 楼层 |
| roomNo | string | 是 | 同楼层唯一 | 房号 |
| displayName | string | 是 | <= 80 字符 | 展示名 |
| capacity | number | 是 | 1-8 | 可住人数 |
| status | string | 是 | available / occupied / maintenance / inactive | 房间状态 |
| version | number | 修改必填 | 乐观锁版本 | 并发控制 |

`tenantId` 和 `facilityId` 由后端认证上下文写入和校验，客户端不得通过请求体覆盖。

### 4.3 POST /api/v1/rooms/{roomId}/assignments

```json
{
  "residentId": "resident-001",
  "bedLabel": "A",
  "roomVersion": 0
}
```

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| residentId | string | 是 | 所属租户、机构内存在 | 长者档案 ID |
| bedLabel | string | 是 | 同房间 active 状态唯一 | 床位标识 |
| roomVersion | number | 否 | 乐观锁版本 | 防止基于旧房态分配 |

## 5. Response

```json
{
  "data": {
    "zones": [
      {
        "id": "zone-hecheng-elderly-care",
        "name": "和成养老",
        "buildings": [
          {
            "id": "building-1",
            "name": "1栋",
            "floors": [
              {
                "id": "floor-1-3",
                "name": "3楼",
                "rooms": [
                  {
                    "id": "room-1-3-301",
                    "roomNo": "301",
                    "displayName": "301号房",
                    "locationLabel": "和成养老 - 1栋 - 3楼 - 301号房",
                    "capacity": 2,
                    "occupiedCount": 1,
                    "status": "available"
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  },
  "traceId": "trace-20260426-room-001"
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data.zones | array | 区-栋-楼-房层级树，首期只有和成养老区 |
| locationLabel | string | 完整居住地点展示文本 |
| traceId | string | 链路追踪 ID |

## 6. Error Codes

| Code | HTTP | 场景 | 前端处理建议 |
| --- | --- | --- | --- |
| ROOM_FORBIDDEN | 403 | 无维护权限 | 禁用维护入口 |
| ROOM_TENANT_SCOPE_DENIED | 403 | 跨租户或跨机构访问 | 清空结果并记录安全告警 |
| ROOM_ZONE_SCOPE_INVALID | 400 | 首期传入非和成养老区 | 提示当前仅支持和成养老 |
| ROOM_HIERARCHY_MISMATCH | 400 | 楼栋、楼层、房间层级不一致 | 要求重新选择层级 |
| ROOM_DUPLICATED | 409 | 同楼层房号重复 | 提示重复房间 |
| ROOM_VERSION_CONFLICT | 409 | 乐观锁冲突 | 刷新后重试 |
| ROOM_OCCUPANCY_CONFLICT | 409 | 房间容量不足或床位已占用 | 提示选择其他房间 |
| ROOM_NOT_FOUND | 404 | 房间不存在或不在授权机构内 | 刷新列表 |
| AUTH_REQUIRED | 401 | 未提供有效 Bearer token | 跳转登录 |
| IDEMPOTENCY_KEY_REUSED | 409 | 幂等键复用但请求体不同 | 重新生成幂等键 |
| IDEMPOTENCY_IN_PROGRESS | 409 | 相同幂等请求仍在处理中 | 稍后重试 |
| RESIDENT_NOT_FOUND | 404 | 长者不存在或不在授权机构内 | 重新选择长者 |
| BED_ASSIGNMENT_CONFLICT | 409 | 长者已有床位或床位已被占用 | 刷新房态后重试 |

## 7. 兼容策略

- 只允许向后兼容新增字段。
- 层级从 `区-栋-楼-房` 变更为更多层级时必须升版本。
- 首期只有和成养老区，新增区需要更新模块文档、测试用例和迁移策略。

## 8. 性能与限流

| 项 | 目标 |
| --- | --- |
| P95 | <= 200ms |
| P99 | <= 500ms |
| QPS 预估 | 单机构 30 QPS |
| 限流键 | tenant_id / user_id / facility_id / IP |
| 缓存策略 | 房间树可短 TTL 缓存；占用数和可住房间查询需读强一致或带版本校验 |

## 9. 测试清单

- 正常请求
- 参数缺失
- 权限不足
- 租户越权
- 非和成养老区请求
- 层级不一致
- 重复房间
- 并发占用冲突
- 停用有入住的房间
- 审计日志
- 幂等创建
- 床位分配同步长者档案

当前自动化覆盖：

- `RoomControllerTest` 覆盖认证必填、房间树、可住房间查询、社工维护越权、同楼层重复房间、层级不一致、幂等创建、床位分配。
