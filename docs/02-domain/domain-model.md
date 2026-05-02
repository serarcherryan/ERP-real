# Domain Model

## 1. 初始业务域地图

| 业务域 | 核心实体 | 说明 | 数据敏感级别 | 首期优先级 |
| --- | --- | --- | --- | --- |
| 机构与租户 | Tenant, Facility, Department | 多机构、多院区、组织结构 | 中 | P0 |
| 用户与权限 | User, Role, Permission, DataScope, StaffProfile | 登录、角色、数据权限、字段权限 | 高 | P0 |
| 长者档案 | Resident, FamilyContact, ResidentTag, HealthSummary | 长者基础资料、联系人、标签、健康摘要 | 高 | P0 |
| 入住生活 | Admission, Contract, HousingZone, Building, Floor, Room, BedAssignment | 房态、住房层级、入住办理、合同、换房、退住 | 高 | P1 |
| 工单中心 | WorkOrder, Assignment, WorkOrderAction, Acceptance | 保洁、维修、服务派工、执行与验收 | 中 | P0 |
| 关怀沟通 | VisitRecord, FamilyCommunication, FollowUpReminder | 一日三巡、入户访视、家属沟通、回访提醒 | 高 | P1 |
| 照护健康 | CarePlan, Assessment, CareRecord, VitalSign | 评估、照护计划、执行记录、生命体征 | 极高 | P1 |
| 药品管理 | Drug, MedicationPlan, DispenseRecord | 用药计划、给药记录、漏服提醒 | 极高 | P2 |
| 意外事件 | Incident, IncidentAction, IncidentClosure | 跌倒、走失、突发疾病上报和闭环 | 极高 | P1 |
| 活动与发布 | Activity, VenueBooking, Announcement, Survey, Club | 活动、场地、公告、问卷、社团 | 中 | P2 |
| 物资库存 | Item, Stock, StockMovement, PurchaseOrder | 用品入库、领用、库存预警、消耗统计 | 中 | P1 |
| 消息通知 | Notification, MessageTemplate, Subscription | 员工提醒、家属通知、待办通知 | 中 | P0 |
| 数据中心 | DashboardMetric, MetricSnapshot, ReportExport | 驾驶舱指标、报表、导出任务 | 敏感 | P0 |
| 审计合规 | AuditLog, SensitiveAccessLog, ComplianceReport | 审计、风险、合规报表 | 高 | P0 |

## 2. 首期核心聚合

### ResidentProfile

```text
聚合名称: ResidentProfile
所属业务域: 长者档案
聚合根: Resident
内部实体: FamilyContact, ResidentTag, ResidentAttachment
值对象: IdentityInfo, ContactInfo, Address, EmergencyContact, HealthSummary
生命周期: Draft -> Active -> Archived
核心不变量:
  - tenant_id 和 facility_id 必须存在
  - 同一租户内证件号、档案号需要按业务规则防重复
  - 家属联系人手机号、身份证等字段需要脱敏展示
禁止的跨聚合直接修改:
  - 工单、照护记录、入住合同不得直接写入长者档案主表
领域事件:
  - resident_profile.updated.v1（契约已定义，当前后端尚未发布）
审计要求:
  - 查看高敏感字段、修改档案、导出档案必须审计
```

### WorkOrder

```text
聚合名称: WorkOrder
所属业务域: 工单中心
聚合根: WorkOrder
内部实体: Assignment, WorkOrderAction, Acceptance, WorkOrderAttachment
值对象: WorkOrderType, Priority, Location, SLA
生命周期: Draft -> Submitted -> Assigned -> InProgress -> Completed -> Accepted -> Closed
允许分支:
  - Submitted -> Cancelled
  - Assigned -> Returned
  - Completed -> Rejected -> InProgress
核心不变量:
  - 状态流转必须合法
  - 重复提交必须通过幂等键识别
  - 验收和退回必须记录原因、操作者和时间
禁止的跨聚合直接修改:
  - 其他模块只能通过工单服务变更工单状态
领域事件:
  - work_order.status_changed.v1
审计要求:
  - 分派、接单、完成、验收、退回、关闭必须审计
```

### RoomManagement

```text
聚合名称: RoomManagement
所属业务域: 入住生活
聚合根: HousingZone / Room
内部实体: Building, Floor, Room, BedAssignment
值对象: RoomLocation, RoomCapacity, RoomStatus
生命周期:
  - HousingZone: Active -> Inactive
  - Room: Available -> Occupied -> Maintenance -> Inactive
核心不变量:
  - 当前首期只有一个活动区: 和成养老
  - 居住空间层级固定为 区 -> 栋 -> 楼 -> 房
  - 同一租户、机构、楼层下 room_no 必须唯一
  - 房间 occupied_count 不得超过 capacity
  - 长者档案只能引用 room_id/bed_id，不得直接维护房间主数据
禁止的跨聚合直接修改:
  - 长者档案不得直接创建、修改、停用房间
  - 工单不得直接修改房间占用数
领域事件:
  - room.occupancy_changed.v1（后端接入床位占用时创建）
审计要求:
  - 创建、修改、停用、容量变更和占用调整必须审计
```

### DashboardMetric

```text
聚合名称: DashboardMetric
所属业务域: 数据中心
聚合根: MetricSnapshot
内部实体: MetricDimension, MetricValue, ReportExport
值对象: MetricCode, TimeRange, DimensionKey
生命周期: Pending -> Ready -> Expired
核心不变量:
  - 指标只读，不得反向写入业务主表
  - 指标口径必须可追溯到版本
  - 导出必须记录范围、原因、操作者和文件过期时间
领域事件:
  - dashboard.metric_refreshed.v1
审计要求:
  - 查看敏感聚合数据、导出报表必须审计
```

## 3. 数据所有权规则

- 每张业务表必须有明确的 owning domain。
- 其他模块只能通过 API、事件或只读投影访问该数据。
- 任何跨域写入必须经过 owning domain 的应用服务。
- 报表和驾驶舱可以使用投影表或分析库，但不得反向写回核心业务表。
- 涉及长者档案、健康、医疗、财务、家属联系方式的数据必须记录访问审计。

## 4. 多租户隔离规则

所有业务数据默认必须带有租户或机构隔离维度：

- `tenant_id`: 集团或客户租户
- `facility_id`: 养老院或院区
- `department_id`: 可选，护理区或业务部门

查询层必须默认注入租户过滤。后台运维账号跨租户访问必须单独审计。

## 5. 权限模型

```text
User -> Role -> Permission
User -> DataScope -> Tenant / Facility / Department / Resident
Permission -> FieldPolicy
```

权限判断至少包含：

- 功能权限：能不能操作
- 数据权限：能操作哪些租户、机构、部门、长者
- 字段权限：能不能看手机号、身份证、健康记录、财务数据
- 场景权限：是否在值班、是否为负责护理区、是否为绑定家属

## 6. 状态机示例

### 入住流程

```text
Draft -> Assessment -> Contracting -> BedAssigned -> Active -> Discharged
```

### 工单流程

```text
Draft -> Submitted -> Assigned -> InProgress -> Completed -> Accepted -> Closed
Submitted -> Cancelled
Assigned -> Returned
Completed -> Rejected -> InProgress
```

状态机变更必须记录操作者、时间、来源端、原因和前后状态。非法状态跳转必须有单元测试和集成测试覆盖。
