# Resident Profile Module

## 1. 模块档案

| 字段 | 内容 |
| --- | --- |
| 模块名称 | 长者档案管理 |
| 所属业务域 | 长者档案 |
| 负责人 | TBD |
| 业务优先级 | P0 |
| 首发端 | Web 管理端 / 员工小程序 |
| 依赖模块 | 机构与租户、用户与权限、入住生活、审计合规、文件服务 |
| 下游影响 | 工单中心、照护健康、关怀沟通、数据中心、消息通知 |

## 2. 业务目标

- 解决的问题：沉淀一人一档，统一长者基础资料、入住状态、联系人、健康摘要和服务记录入口。
- 目标用户：社工、社工主管、部门经理、物业主管、管理层。
- 成功指标：档案完整率、重复档案率、档案变更审计覆盖率、档案查询 P95。
- 不做范围：完整医疗病历、完整账单明细、复杂照护计划编排。

## 3. 核心流程

```text
创建档案 -> 录入基础信息 -> 绑定房间/床位/入住状态 -> 维护家属联系人 -> 维护标签与健康摘要 -> 查看服务时间线
```

## 4. 领域模型

| 实体/聚合 | 说明 | 所有者 | 生命周期 |
| --- | --- | --- | --- |
| Resident | 长者档案聚合根 | 长者档案 | Draft / Active / Archived |
| FamilyContact | 家属或紧急联系人 | 长者档案 | Active / Inactive |
| ResidentTag | 长者标签，如失智、独居、重点关注 | 长者档案 | Active / Deleted |
| ResidentAttachment | 身份证、合同摘要、照片等附件引用 | 长者档案 | Active / Deleted |
| HealthSummary | 健康摘要，只保存概览和引用 | 长者档案 | Active |

## 5. 数据模型草案

| 表/集合 | 用途 | 关键字段 | 索引 | 数据量预估 |
| --- | --- | --- | --- | --- |
| residents | 长者基础档案 | id, tenant_id, facility_id, resident_no, name, gender, birth_date, status | tenant_id + facility_id + resident_no, tenant_id + name | 每机构千级到万级 |
| family_contacts | 家属联系人 | id, tenant_id, facility_id, resident_id, name, relation, phone, is_emergency | resident_id, tenant_id + phone_hash | 每长者 1-5 条 |
| resident_tags | 长者标签 | id, tenant_id, facility_id, resident_id, tag_code | resident_id + tag_code | 每长者 0-20 条 |
| resident_attachments | 附件引用 | id, tenant_id, facility_id, resident_id, file_id, category | resident_id + category | 按附件量增长 |
| resident_profile_snapshots | 档案摘要投影 | resident_id, summary_json, updated_at | tenant_id + facility_id, updated_at | 与长者数量同级 |

## 6. API 与事件

| 类型 | 名称 | 调用方/订阅方 | 契约文件 |
| --- | --- | --- | --- |
| API | Resident Profile CRUD | Web 管理端、员工小程序、BFF | 待创建 |
| API | Resident Timeline Query | Web 管理端、驾驶舱 BFF | 待创建 |
| Event | resident_profile.updated.v1 | 数据中心、搜索、审计、通知 | `docs/03-apis/resident-profile-updated-event.md` |

## 7. 权限与数据范围

| 操作 | 角色 | 数据范围 | 审计 |
| --- | --- | --- | --- |
| 创建 | 社工、社工主管 | 所属租户、机构、部门 | 是 |
| 查看 | 社工、社工主管、部门经理、物业主管 | 授权机构、部门或绑定长者 | 高敏感字段是 |
| 修改 | 社工、社工主管 | 授权机构、部门 | 是 |
| 删除/作废 | 社工主管、部门经理 | 授权机构，需原因 | 是 |
| 导出 | 部门经理、授权管理员 | 授权机构，需原因和范围 | 是 |

## 8. 并发与一致性

- 是否存在重复提交：存在，创建档案需要通过证件号 hash、档案号、幂等键防重。
- 是否需要乐观锁：需要，档案编辑使用 `version` 字段。
- 是否需要分布式锁：默认不需要，批量导入或合并档案时按场景评估。
- 是否允许最终一致：档案摘要、搜索索引、驾驶舱指标允许最终一致。
- 失败补偿方式：事件重试、死信队列、投影重建任务。

## 9. 可观测性

| 类型 | 内容 |
| --- | --- |
| 日志 | residentId、tenantId、facilityId、operatorId、traceId，不记录明文身份证和手机号 |
| 指标 | 查询量、创建量、修改量、错误率、P95、投影延迟 |
| 告警 | P0 接口错误率、投影重建失败、审计写入失败 |
| 审计 | 高敏感字段查看、修改、作废、导出 |

## 10. 测试计划

- 单元测试：档案状态、证件号防重、字段脱敏、权限判断。
- 集成测试：租户隔离、乐观锁冲突、事件发布、审计写入。
- 契约测试：档案 CRUD、列表分页、时间线查询。
- E2E 测试：创建档案、修改联系人、查看时间线。
- 性能测试：档案列表分页、模糊搜索、摘要加载。
- 权限/越权测试：跨租户、跨机构、字段级权限。
- 数据迁移测试：批量导入和投影重建。

## 11. 发布计划

- Feature flag：按租户和机构开启。
- 灰度范围：先单机构社工条线，再扩展到物业视图只读。
- 数据迁移：如导入历史档案，先导入主表，再生成摘要投影。
- 回滚方式：关闭入口，保留数据，按迁移批次回滚。
- 监控观察窗口：首发后至少 7 天观察档案查询、修改、审计和投影延迟。
