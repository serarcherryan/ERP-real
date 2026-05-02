# Release And Change Management

## 1. 环境策略

| 环境 | 用途 | 数据 | 发布权限 |
| --- | --- | --- | --- |
| local | 本地开发 | 合成数据 | 开发 |
| dev | 联调 | 合成或脱敏数据 | 开发 |
| test | QA 验证 | 稳定测试数据 | QA/开发 |
| staging | 预发布 | 接近生产的脱敏数据 | Tech Lead |
| production | 生产 | 真实数据 | Release Owner |

当前实现约定：

- 本地/开发后端默认使用 `SPRING_PROFILES_ACTIVE=dev`，PostgreSQL 由 `docker/dev/docker-compose.yml` 启动。
- 自动化测试使用 `test` profile，采用 H2 PostgreSQL mode + Flyway，避免依赖本机数据库状态。
- 生产使用 `prod` profile，必须通过环境变量注入 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`，并提供 `ERP_AUTH_JWT_SECRET` 或等价安全配置。生产数据库拓扑、密钥管理、高可用、备份、身份源和部署平台尚未固化，后续需要独立 ADR。
- `dev/test` profile 内置开发 Bearer token fallback；`prod` 不内置开发 token。
- 当前 `V4__sys_users.sql` 会初始化 5 个演示账号，初始密码为 `Erp@2026`；生产发布前必须替换初始化策略或强制首登改密。

## 2. 发布清单

```text
版本号:
负责人:
发布日期:
影响模块:
影响端:
数据库变更:
配置变更:
Feature flag:
灰度范围:
回滚方案:
验证人:
观察窗口:
```

## 3. 变更等级

| 等级 | 定义 | 要求 |
| --- | --- | --- |
| Low | 文案、样式、小修复 | PR 审查 + 基础验证 |
| Medium | 普通业务功能 | 测试用例 + 回滚说明 |
| High | 核心流程、权限、数据迁移 | 评审 + 灰度 + 监控 |
| Critical | 支付、医疗、审计、多租户隔离 | 发布会签 + 演练 + 应急预案 |

## 4. 数据迁移模板

```text
迁移目的:
影响表:
数据量:
是否在线迁移:
是否可回滚:
迁移步骤:
校验 SQL/脚本:
失败处理:
观察指标:
```

## 5. 回滚策略

- 代码回滚：明确回滚版本和兼容性。
- 配置回滚：记录配置项、旧值、新值。
- 数据回滚：优先通过补偿脚本，避免盲目恢复全库。
- Feature flag：核心功能尽量支持快速关闭。
- 事件回放：异步链路需要明确是否可重放。

## 6. 发布后观察

| 指标 | 观察时间 |
| --- | --- |
| 错误率 | 30-60 分钟 |
| P95/P99 | 30-60 分钟 |
| DB 慢查询 | 30-60 分钟 |
| 队列积压 | 30-60 分钟 |
| 核心业务成功率 | 1 个业务周期 |
| 用户反馈 | 1-3 天 |
