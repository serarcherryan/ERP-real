# Resident Profile Data Schema

## 1. 设计原则

- 所有长者档案数据必须带 `tenant_id` 和 `facility_id`。
- 身份证、手机号、详细住址等高敏感字段不以明文存储；数据库保存密文和可检索 hash。
- `resident_profiles` 保存一人一档的主信息和入住摘要；联系人、健康风险、标签拆表管理。
- 附件和档案摘要投影尚未落库；接入文件服务、搜索或驾驶舱投影时需补充迁移和契约。
- 工单、照护、医疗、财务和合同明细不得直接写入长者档案主表，只能保存摘要、引用或投影。
- 修改档案必须使用 `version` 乐观锁，并写审计日志。

## 2. resident_profiles

| 字段 | 类型 | 必填 | 敏感级别 | 说明 |
| --- | --- | --- | --- | --- |
| id | uuid | 是 | 普通 | 主键 |
| tenant_id | uuid/string | 是 | 敏感 | 租户隔离 |
| facility_id | uuid/string | 是 | 敏感 | 机构隔离 |
| department_id | uuid/string | 是 | 敏感 | 养老部门或护理区 |
| resident_no | varchar(40) | 是 | 敏感 | 档案号，同租户唯一 |
| name | varchar(80) | 是 | 高敏感 | 姓名 |
| preferred_name | varchar(80) | 否 | 敏感 | 常用称呼 |
| gender | varchar(20) | 是 | 敏感 | male / female / unknown |
| birth_date | date | 是 | 高敏感 | 出生日期 |
| identity_type | varchar(40) | 是 | 高敏感 | 居民身份证、护照等 |
| identity_no_cipher | text | 是 | 极高敏感 | 证件号密文 |
| identity_no_hash | varchar(128) | 是 | 极高敏感 | 证件号 hash，用于防重 |
| phone_cipher | text | 否 | 高敏感 | 长者联系电话密文 |
| phone_hash | varchar(128) | 否 | 高敏感 | 电话 hash |
| household_address_cipher | text | 否 | 高敏感 | 户籍地址密文 |
| current_address_cipher | text | 否 | 高敏感 | 现住址密文 |
| status | varchar(30) | 是 | 敏感 | Draft / Active / Archived |
| admission_status | varchar(30) | 是 | 敏感 | 待入住、在住、暂离、退住 |
| admission_date | date | 否 | 敏感 | 入住日期 |
| contract_no | varchar(80) | 否 | 敏感 | 合同编号，只存摘要标识 |
| zone_id | uuid/string | 否 | 敏感 | 居住区引用，首期为和成养老 |
| building_id | uuid/string | 否 | 敏感 | 楼栋引用 |
| floor_id | uuid/string | 否 | 敏感 | 楼层引用 |
| room_id | uuid/string | 否 | 敏感 | 房间引用，归房间管理模块所有 |
| bed_id | uuid/string | 否 | 敏感 | 床位引用 |
| room_label | varchar(120) | 否 | 敏感 | 展示冗余，如 3F-301 |
| bed_label | varchar(40) | 否 | 敏感 | 展示冗余 |
| living_location_label | varchar(240) | 否 | 敏感 | 房间管理生成的完整区-栋-楼-房展示冗余 |
| nursing_zone | varchar(80) | 否 | 敏感 | 护理区 |
| care_level | varchar(40) | 否 | 高敏感 | 护理等级 |
| payment_type | varchar(40) | 否 | 敏感 | 月付、季付等摘要 |
| medical_insurance_type | varchar(80) | 否 | 高敏感 | 医保类型摘要 |
| responsible_social_worker_id | uuid/string | 否 | 敏感 | 责任社工 |
| health_summary | varchar(500) | 否 | 极高敏感 | 健康摘要，不保存完整病历 |
| care_needs_text | varchar(1000) | 否 | 高敏感 | 照护需求摘要，当前以分隔文本持久化 |
| completeness_score | numeric(5,2) | 是 | 敏感 | 档案完整度 |
| missing_fields_text | varchar(1000) | 否 | 敏感 | 待补字段，当前以分隔文本持久化 |
| last_service_at | timestamp | 否 | 敏感 | 最近服务时间 |
| next_follow_up_date | date | 否 | 敏感 | 下次跟进日期 |
| created_by | uuid/string | 是 | 敏感 | 创建人 |
| updated_by | uuid/string | 是 | 敏感 | 更新人 |
| created_at | timestamp | 是 | 普通 | 创建时间 |
| updated_at | timestamp | 是 | 普通 | 更新时间 |
| archived_at | timestamp | 否 | 敏感 | 归档/作废时间 |
| archived_reason | varchar(500) | 否 | 敏感 | 归档/作废原因 |
| version | bigint | 是 | 普通 | 乐观锁版本 |

推荐索引：

- `uk_resident_profiles_tenant_no(tenant_id, resident_no)`
- `uk_resident_profiles_tenant_identity(tenant_id, identity_no_hash)`
- `idx_resident_profiles_facility_status(tenant_id, facility_id, status)`
- `idx_resident_profiles_room(tenant_id, facility_id, room_id)`
- `idx_resident_profiles_updated_at(tenant_id, facility_id, updated_at)`

## 3. family_contacts

| 字段 | 类型 | 必填 | 敏感级别 | 说明 |
| --- | --- | --- | --- | --- |
| id | uuid | 是 | 普通 | 主键 |
| tenant_id | uuid/string | 是 | 敏感 | 租户隔离 |
| facility_id | uuid/string | 是 | 敏感 | 机构隔离 |
| resident_id | uuid | 是 | 高敏感 | 长者档案 ID |
| name | varchar(80) | 是 | 高敏感 | 联系人姓名 |
| relation | varchar(40) | 是 | 高敏感 | 与长者关系 |
| phone_cipher | text | 是 | 高敏感 | 电话密文 |
| phone_hash | varchar(128) | 是 | 高敏感 | 电话 hash |
| address_cipher | text | 否 | 高敏感 | 联系地址密文 |
| is_emergency | boolean | 是 | 敏感 | 是否紧急联系人 |
| is_guardian | boolean | 是 | 高敏感 | 是否监护/委托人 |
| can_receive_notice | boolean | 是 | 敏感 | 是否接收通知 |
| priority | int | 是 | 普通 | 联系优先级 |
| status | varchar(30) | 是 | 普通 | Active / Inactive |
| created_at | timestamp | 是 | 普通 | 创建时间 |
| updated_at | timestamp | 是 | 普通 | 更新时间 |

## 4. resident_health_summaries

| 字段 | 类型 | 必填 | 敏感级别 | 说明 |
| --- | --- | --- | --- | --- |
| id | uuid | 是 | 普通 | 主键 |
| tenant_id | uuid/string | 是 | 敏感 | 租户隔离 |
| facility_id | uuid/string | 是 | 敏感 | 机构隔离 |
| resident_id | uuid | 是 | 极高敏感 | 长者档案 ID |
| blood_type | varchar(20) | 否 | 高敏感 | 血型 |
| allergy_summary_text | varchar(1000) | 否 | 极高敏感 | 过敏摘要，当前以分隔文本持久化 |
| chronic_disease_summary_text | varchar(1000) | 否 | 极高敏感 | 慢病摘要，当前以分隔文本持久化 |
| mobility_level | varchar(80) | 否 | 高敏感 | 行动能力 |
| cognitive_status | varchar(80) | 否 | 极高敏感 | 认知状态 |
| diet_requirement | varchar(120) | 否 | 高敏感 | 饮食要求 |
| fall_risk_level | varchar(20) | 否 | 高敏感 | 跌倒风险 |
| emergency_plan | varchar(500) | 否 | 极高敏感 | 应急预案摘要 |
| version | bigint | 是 | 普通 | 乐观锁版本 |
| created_at | timestamp | 是 | 普通 | 创建时间 |
| updated_at | timestamp | 是 | 普通 | 更新时间 |

## 5. resident_tags

| 字段 | 类型 | 必填 | 敏感级别 | 说明 |
| --- | --- | --- | --- | --- |
| id | uuid | 是 | 普通 | 主键 |
| tenant_id | uuid/string | 是 | 敏感 | 租户隔离 |
| facility_id | uuid/string | 是 | 敏感 | 机构隔离 |
| resident_id | uuid | 是 | 高敏感 | 长者档案 ID |
| tag_code | varchar(80) | 是 | 敏感 | 标签编码 |
| tag_name | varchar(80) | 是 | 敏感 | 标签名称 |
| source | varchar(40) | 是 | 普通 | manual / assessment / system |
| status | varchar(30) | 是 | 普通 | Active / Deleted |
| created_at | timestamp | 是 | 普通 | 创建时间 |

## 6. 后续表：resident_attachments

当前后端尚未创建 `resident_attachments` 表。接入文件服务或身份证、合同、评估附件时，需先补充 API/权限/审计契约和 Flyway 迁移。

| 字段 | 类型 | 必填 | 敏感级别 | 说明 |
| --- | --- | --- | --- | --- |
| id | uuid | 是 | 普通 | 主键 |
| tenant_id | uuid/string | 是 | 敏感 | 租户隔离 |
| facility_id | uuid/string | 是 | 敏感 | 机构隔离 |
| resident_id | uuid | 是 | 高敏感 | 长者档案 ID |
| file_id | uuid/string | 是 | 高敏感 | 文件服务 ID |
| category | varchar(60) | 是 | 高敏感 | 身份证、合同、评估、授权委托书等 |
| file_name | varchar(200) | 是 | 高敏感 | 文件名 |
| status | varchar(30) | 是 | 普通 | Active / Deleted |
| uploaded_by | uuid/string | 是 | 敏感 | 上传人 |
| uploaded_at | timestamptz | 是 | 普通 | 上传时间 |

## 7. 后续表：resident_profile_snapshots

当前后端尚未创建 `resident_profile_snapshots` 表。列表页直接查询 `resident_profiles`、`resident_health_summaries` 和 `resident_tags`；接入搜索、驾驶舱或异步投影时再创建。

| 字段 | 类型 | 必填 | 敏感级别 | 说明 |
| --- | --- | --- | --- | --- |
| resident_id | uuid | 是 | 高敏感 | 长者档案 ID |
| tenant_id | uuid/string | 是 | 敏感 | 租户隔离 |
| facility_id | uuid/string | 是 | 敏感 | 机构隔离 |
| summary_json | jsonb | 是 | 高敏感 | 列表和搜索摘要投影，必须脱敏 |
| completeness_score | numeric(5,2) | 是 | 敏感 | 档案完整度 |
| projection_version | bigint | 是 | 普通 | 投影版本 |
| updated_at | timestamptz | 是 | 普通 | 更新时间 |

## 8. 不建议进入长者档案主表的数据

- 完整医疗病历、诊断报告、用药明细：由照护健康或药品管理模块拥有。
- 账单、支付、押金和结算明细：由财务或收费模块拥有。
- 工单明细和执行记录：由工单中心拥有。
- 合同原文和附件二进制：由合同/文件服务拥有，档案只保存引用。
