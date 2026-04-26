# Resident Profile Management API Contract

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| API 名称 | Resident Profile Management |
| 所属模块 | 长者档案管理 |
| 负责人 | TBD |
| 版本 | v1 |
| 调用端 | Web 管理端 / 员工端 / BFF |
| 风险等级 | P0 |
| 数据敏感级别 | 高敏感 |

## 2. Endpoint

```http
GET /api/v1/residents
POST /api/v1/residents
GET /api/v1/residents/{residentId}
PATCH /api/v1/residents/{residentId}
POST /api/v1/residents/{residentId}/void
POST /api/v1/residents/export-tasks
```

## 3. 业务说明

- 用户故事：Web 管理端用户按角色查看、创建、修改、作废和导出授权范围内的长者档案。
- 前置条件：用户已认证，权限服务已返回功能权限、数据范围和字段权限。
- 后置结果：档案写入长者档案模块，敏感查看、修改、作废和导出写入审计。
- 幂等要求：创建接口必须支持 `Idempotency-Key`，按租户、证件号 hash、档案号防重复。
- 权限要求：社工和社工主管可创建/修改；社工主管和部门经理可作废；部门经理、物业经理和授权管理员可导出；物业角色默认只读且敏感字段脱敏。
- 审计要求：查看明文敏感字段、修改、作废、导出必须记录 operatorId、tenantId、facilityId、residentId、原因和 traceId。

## 4. Request

### 4.1 GET /api/v1/residents

```json
{
  "tenantId": "tenant-yiyang",
  "facilityId": "facility-east",
  "departmentId": "dept-care-a",
  "keyword": "陈兰英",
  "status": "Active",
  "page": 1,
  "pageSize": 20
}
```

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| tenantId | string | 是 | 来自认证上下文 | 租户隔离维度 |
| facilityId | string | 否 | 必须在用户数据范围内 | 机构过滤 |
| departmentId | string | 否 | 必须在用户数据范围内 | 部门过滤 |
| keyword | string | 否 | <= 50 字符 | 姓名、档案号、房间、标签搜索 |
| status | string | 否 | Draft / Active / Archived | 档案状态 |
| page | number | 是 | >= 1 | 页码 |
| pageSize | number | 是 | 1-100 | 分页大小 |

### 4.2 POST/PATCH /api/v1/residents

```json
{
  "tenantId": "tenant-yiyang",
  "facilityId": "facility-east",
  "residentNo": "CY-2026-0001",
  "name": "陈兰英",
  "preferredName": "陈阿姨",
  "gender": "female",
  "birthDate": "1944-05-12",
  "identityType": "居民身份证",
  "identityNo": "310101194405126428",
  "phone": "13821886721",
  "householdAddress": "上海市黄浦区外滩街道",
  "currentAddress": "东区颐养中心 3F 护理一区",
  "admission": {
    "admissionStatus": "admitted",
    "admissionDate": "2024-09-12",
    "contractNo": "HT-2024-0912-001",
    "roomId": "room-301",
    "bedId": "bed-301-a",
    "room": "3F-护理一区-301",
    "bed": "A床",
    "nursingZone": "护理一区",
    "careLevel": "二级护理",
    "paymentType": "月付",
    "medicalInsuranceType": "城镇职工医保",
    "responsibleSocialWorkerId": "staff-001",
    "caseManagerId": "staff-020"
  },
  "familyContacts": [
    {
      "name": "陈思远",
      "relation": "儿子",
      "phone": "13917223455",
      "address": "上海市浦东新区陆家嘴街道",
      "isEmergency": true,
      "isGuardian": true,
      "canReceiveNotice": true,
      "priority": 1
    }
  ],
  "health": {
    "bloodType": "A型",
    "allergyHistory": ["青霉素"],
    "chronicDiseases": ["高血压", "骨质疏松"],
    "mobilityLevel": "扶手杖辅助行走",
    "cognitiveStatus": "轻度记忆下降",
    "dietRequirement": "低盐软食",
    "fallRiskLevel": "high",
    "pressureSoreRiskLevel": "medium",
    "emergencyPlan": "夜间离床触发巡查"
  },
  "healthSummary": "高血压稳定，需关注夜间睡眠和跌倒风险。",
  "careNeeds": ["夜间巡查", "跌倒预防", "慢病随访"],
  "tags": ["重点关注", "慢病"],
  "version": 7
}
```

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| tenantId | string | 是 | 来自认证上下文 | 租户隔离维度 |
| facilityId | string | 是 | 必须在用户数据范围内 | 机构隔离维度 |
| residentNo | string | 创建必填 | 同租户唯一 | 档案号 |
| name | string | 是 | 1-40 字符 | 长者姓名 |
| preferredName | string | 否 | <= 40 字符 | 常用称呼 |
| gender | string | 是 | male / female | 性别 |
| birthDate | string | 是 | ISO 日期 | 出生日期 |
| identityType | string | 是 | 字典值 | 证件类型 |
| identityNo | string | 创建必填 | 后端加密存储并生成 hash | 证件号 |
| phone | string | 否 | 后端加密存储并生成 hash | 长者联系电话 |
| householdAddress | string | 否 | 后端加密存储 | 户籍地址 |
| currentAddress | string | 否 | 后端加密存储 | 现住址 |
| admission | object | 是 | 见数据 schema | 入住、合同、房间床位、医保缴费摘要 |
| familyContacts | array | 否 | 1-5 条 | 家属联系人 |
| health | object | 否 | 只保存摘要和风险等级 | 健康风险摘要，不保存完整病历 |
| healthSummary | string | 否 | <= 500 字符 | 健康摘要，不保存完整病历 |
| careNeeds | array | 否 | <= 20 条 | 照护需求摘要 |
| tags | array | 否 | <= 20 条 | 长者标签 |
| version | number | 修改必填 | 乐观锁版本 | 防止并发覆盖 |

## 5. Response

```json
{
  "data": {
    "items": [
      {
        "id": "res-001",
        "tenantId": "tenant-yiyang",
        "facilityId": "facility-east",
        "residentNo": "CY-2026-0001",
        "name": "陈兰英",
        "status": "Active",
        "maskedPhone": "138****6721",
        "maskedIdentityNo": "310101********6428",
        "admissionStatus": "admitted",
        "room": "3F-护理一区-301",
        "bed": "A床",
        "careLevel": "二级护理",
        "fallRiskLevel": "high",
        "pressureSoreRiskLevel": "medium",
        "completenessScore": 96,
        "missingFields": [],
        "tags": ["重点关注", "慢病"],
        "version": 7
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1
  },
  "traceId": "trace-20260426-001"
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data.items | array | 档案列表，字段根据角色权限脱敏 |
| data.page | number | 当前页码 |
| data.pageSize | number | 分页大小 |
| data.total | number | 总数 |
| traceId | string | 链路追踪 ID |

## 6. Error Codes

| Code | HTTP | 场景 | 前端处理建议 |
| --- | --- | --- | --- |
| RESIDENT_PROFILE_FORBIDDEN | 403 | 功能权限或数据范围不足 | 禁用操作并提示联系主管 |
| RESIDENT_PROFILE_TENANT_SCOPE_DENIED | 403 | 跨租户/跨机构访问 | 清空结果并记录安全告警 |
| RESIDENT_PROFILE_DUPLICATED | 409 | 证件号 hash 或档案号重复 | 展示疑似重复档案入口 |
| RESIDENT_PROFILE_VERSION_CONFLICT | 409 | 乐观锁版本冲突 | 提示刷新后重试 |
| RESIDENT_PROFILE_EXPORT_REASON_REQUIRED | 400 | 导出缺少原因 | 打开导出原因表单 |
| RESIDENT_PROFILE_RATE_LIMITED | 429 | 查询或导出触发限流 | 显示稍后重试 |

## 7. 兼容策略

- 只允许向后兼容新增字段。
- 字段删除、语义变化、枚举收紧必须升版本。
- 响应字段必须说明是否可为空。
- 时间字段统一使用 ISO 8601。
- 涉及金额的后续扩展必须使用 Decimal 字符串或最小货币单位。

## 8. 性能与限流

| 项 | 目标 |
| --- | --- |
| P95 | <= 300ms |
| P99 | <= 800ms |
| QPS 预估 | 单机构 50 QPS，集团视图按 BFF 聚合缓存 |
| 限流键 | tenant_id / user_id / facility_id / IP |
| 缓存策略 | 字典和权限缓存；档案列表按搜索条件短 TTL；敏感字段不缓存到浏览器持久存储 |

## 9. 测试清单

- 正常请求
- 参数缺失
- 权限不足
- 租户越权
- 幂等重复提交
- 并发冲突
- 审计日志
- 降级或异常路径
