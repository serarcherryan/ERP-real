# Room Management Module

## 1. 模块档案

| 字段 | 内容 |
| --- | --- |
| 模块名称 | 房间管理 |
| 所属业务域 | 入住生活 |
| 负责人 | TBD |
| 业务优先级 | P0 |
| 首发端 | Web 管理端 / 后端服务 |
| 依赖模块 | 机构与租户、用户与权限、审计合规 |
| 下游影响 | 长者档案、入住办理、工单中心、数据中心 |

## 2. 业务目标

- 解决的问题：为长者档案和入住办理提供统一、可校验的居住空间层级，避免档案直接手写房间文本。
- 目标用户：社工、社工主管、部门经理、物业经理、物业主管。
- 成功指标：房间引用准确率、重复房间率、床位占用准确率、房间查询 P95。
- 不做范围：复杂楼宇 BIM、门禁设备、能耗计量、完整资产折旧。

## 3. 核心流程

```text
初始化和成养老区 -> 维护栋 -> 维护楼 -> 维护房间 -> 查询可住房间 -> 长者档案/入住办理引用房间
```

当前住房区域层级固定为：

```text
区 -> 栋 -> 楼 -> 房
```

首期只有一个区：

```text
和成养老
```

长者居住地点展示格式：

```text
和成养老 - 1栋 - 3楼 - 301号房
```

## 4. 领域模型

| 实体/聚合 | 说明 | 所有者 | 生命周期 |
| --- | --- | --- | --- |
| HousingZone | 居住区，首期仅和成养老 | 房间管理 | Active / Inactive |
| Building | 楼栋 | 房间管理 | Active / Inactive |
| Floor | 楼层 | 房间管理 | Active / Inactive |
| Room | 房间 | 房间管理 | Available / Occupied / Maintenance / Inactive |
| BedAssignment | 房间/床位与长者的占用关系 | 入住生活 | Reserved / Active / Released |

## 5. 数据模型草案

| 表/集合 | 用途 | 关键字段 | 索引 | 数据量预估 |
| --- | --- | --- | --- | --- |
| housing_zones | 居住区 | id, tenant_id, facility_id, name, status | tenant_id + facility_id + name | 首期 1 条 |
| buildings | 楼栋 | id, tenant_id, facility_id, zone_id, name, sort_order | zone_id + name | 每区个位到十位 |
| floors | 楼层 | id, tenant_id, facility_id, zone_id, building_id, floor_no, name | building_id + floor_no | 每栋十级以内 |
| rooms | 房间 | id, tenant_id, facility_id, zone_id, building_id, floor_id, room_no, capacity, occupied_count, status | floor_id + room_no, tenant_id + facility_id + status | 每机构百级到千级 |
| bed_assignments | 床位占用 | id, tenant_id, facility_id, resident_id, room_id, bed_label, status, start_at, end_at | resident_id + status, room_id + status | 与入住记录同级 |
| audit_logs | 业务审计 | id, tenant_id, facility_id, module_name, action, resource_type, resource_id, operator_id, operator_role, trace_id | resource, created_at | 随关键操作增长 |
| idempotency_records | 幂等记录 | id, tenant_id, facility_id, idempotency_key, action, request_hash, resource_id, status | tenant_id + facility_id + action + idempotency_key | 与写请求同级 |

## 6. API 与事件

| 类型 | 名称 | 调用方/订阅方 | 契约文件 |
| --- | --- | --- | --- |
| API | Room Management | Web 管理端、长者档案 BFF、入住办理 BFF | `docs/03-apis/room-management.md` |
| Event | room.occupancy_changed.v1 | 长者档案、数据中心、工单中心 | 待后端接入床位占用时创建 |

当前后端实现位于 `apps/backend/src/main/java/com/erpreal/backend/room`，数据库结构由 Flyway 管理：

- `V1__room_management_schema.sql`：创建 `housing_zones`、`buildings`、`floors`、`rooms`。
- `V2__room_management_seed.sql`：初始化和成养老区、示例楼栋、楼层和房间。
- `V3__governance_idempotency_and_assignments.sql`：创建审计、幂等、长者房间引用和床位分配表。

## 7. 权限与数据范围

| 操作 | 角色 | 数据范围 | 审计 |
| --- | --- | --- | --- |
| 创建 | 物业经理、物业主管、部门经理 | 所属租户、机构 | 是 |
| 查看 | 社工、社工主管、部门经理、物业经理、物业主管 | 授权机构 | 否 |
| 修改 | 物业经理、物业主管、部门经理 | 所属租户、机构 | 是 |
| 删除/停用 | 物业经理、部门经理 | 所属租户、机构，且无有效入住 | 是 |
| 床位分配 | 社工、社工主管、部门经理 | 所属租户、机构 | 是 |

## 8. 并发与一致性

- 是否存在重复提交：存在，创建房间需要按 `tenant_id + facility_id + floor_id + room_no` 防重，并通过 `idempotency_records` 支持 `Idempotency-Key`。
- 是否需要乐观锁：需要，房间容量、状态和占用数更新使用 `version`。
- 是否需要分布式锁：首期单体内通过房间行级悲观锁和 active 床位唯一约束防并发重复占用；拆服务后再升级为分布式锁或串行化命令。
- 是否允许最终一致：床位分配时长者档案 `roomId/bedLabel/livingLocationLabel` 与房间占用数同事务更新；后续展示投影可异步重建。
- 失败补偿方式：占用变更事件重试、房态投影重建、异常占用巡检任务。

## 9. 可观测性

| 类型 | 内容 |
| --- | --- |
| 日志 | roomId、zoneId、buildingId、floorId、tenantId、facilityId、operatorId、traceId |
| 指标 | 房间查询量、可住房间数、满房率、维护房间数、占用冲突数 |
| 告警 | 重复房间、占用数超过容量、有效入住引用停用房间 |
| 审计 | 创建、修改、停用、容量变更、床位分配、占用释放，落库到 `audit_logs` |

## 10. 测试计划

- 单元测试：区-栋-楼-房层级校验、和成养老唯一活动区、居住地点格式化、可入住房间判断。
- 集成测试：租户/机构隔离、重复房间防重、容量并发占用、停用房间限制。
- 契约测试：房间列表、层级树、房间详情、创建/修改、可住房间查询。
- E2E 测试：长者档案选择房间并展示完整居住地点。
- 性能测试：房间树加载、可住房间查询。
- 权限/越权测试：物业角色维护，养老角色只读。
- 幂等测试：同一 `Idempotency-Key` 重放返回同一资源，不同请求体返回冲突。
- 床位分配测试：房间行级锁、床位唯一、长者档案 roomId 同步。
- 数据迁移测试：历史房间文本拆分为区、栋、楼、房引用。

## 11. 发布计划

- Feature flag：按租户和机构开启房间引用校验。
- 灰度范围：先导入和成养老区及现有楼栋楼层房间，再切换长者档案引用。
- 数据迁移：历史房间文本需要映射到 `zone_id/building_id/floor_id/room_id`。
- 回滚方式：保留原 room_label 展示字段，关闭强校验后允许只读查看。
- 监控观察窗口：上线后至少 7 天观察重复房间、占用冲突和档案房间引用错误。
