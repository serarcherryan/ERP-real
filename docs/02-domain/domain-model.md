# Domain Model

## 1. 初始业务域地图

| 业务域 | 核心实体 | 说明 | 数据敏感级别 |
| --- | --- | --- | --- |
| 机构与租户 | Tenant, Facility, Department | 多机构、多院区、组织结构 | 中 |
| 用户与权限 | User, Role, Permission, StaffProfile | 登录、角色、数据权限 | 高 |
| 老人档案 | Resident, FamilyContact, HealthProfile | 老人基础资料、联系人、健康档案 | 高 |
| 入住退住 | Admission, Contract, BedAssignment, Discharge | 入住评估、床位、合同、退住 | 高 |
| 护理服务 | CarePlan, CareTask, CareRecord | 护理计划、任务、执行记录 | 高 |
| 医疗健康 | VitalSign, MedicalRecord, MedicationOrder | 生命体征、病历、用药 | 极高 |
| 药品管理 | Drug, Prescription, DispenseRecord | 药品库存、发药、复核 | 极高 |
| 餐饮营养 | MealPlan, DietRestriction, MealRecord | 餐食计划、禁忌、营养记录 | 中 |
| 财务收费 | Bill, ChargeItem, Payment, Refund | 账单、支付、退款、对账 | 高 |
| 库存资产 | Item, Stock, PurchaseOrder, Asset | 耗材、资产、采购、盘点 | 中 |
| 人事排班 | Staff, Shift, Attendance, Leave | 员工、班次、考勤、请假 | 中 |
| 消息通知 | Notification, MessageTemplate, Subscription | 家属通知、员工告警 | 中 |
| 审计合规 | AuditLog, RiskEvent, ComplianceReport | 审计、风险、合规报表 | 高 |

## 2. 聚合设计模板

```text
聚合名称:
所属业务域:
聚合根:
内部实体:
值对象:
生命周期:
核心不变量:
允许的状态流转:
禁止的跨聚合直接修改:
领域事件:
审计要求:
```

## 3. 数据所有权规则

- 每张业务表必须有明确的 owning domain。
- 其他模块只能通过 API、事件或只读投影访问该数据。
- 任何跨域写入必须经过 owning domain 的应用服务。
- 报表可以使用投影表或分析库，但不得反向写回核心业务表。
- 涉及老人健康、医疗、财务的数据必须记录访问审计。

## 4. 多租户隔离规则

所有业务数据默认必须带有租户或机构隔离维度：

- `tenant_id`: 集团或客户租户
- `facility_id`: 养老院或院区
- `department_id`: 可选，护理区或业务部门

查询层必须默认注入租户过滤。后台运维账号跨租户访问必须单独审计。

## 5. 状态机示例

### 入住流程

```text
Draft -> Assessment -> Contracting -> BedAssigned -> Active -> Discharged
```

### 护理任务

```text
Pending -> Assigned -> InProgress -> Completed
Pending -> Cancelled
Assigned -> Missed
```

状态机变更必须记录操作者、时间、来源端、原因和前后状态。

