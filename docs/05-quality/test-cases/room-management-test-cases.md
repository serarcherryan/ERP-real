# Room Management Test Cases

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 所属模块 | 房间管理 |
| 影响端 | 后端服务 / Web 管理端 / 长者档案 |
| 风险等级 | P0 |
| 关联需求/契约 | `docs/04-modules/room-management.md`, `docs/03-apis/room-management.md` |
| 负责人 | TBD |

## 2. 测试范围

- 覆盖范围：区-栋-楼-房层级、和成养老唯一活动区、居住地点格式化、房间层级查询、可入住房间判断、长者档案房间引用、认证上下文、审计落库、幂等创建、床位分配。
- 不覆盖范围：多线程并发压测、生产身份源、审计检索界面。
- 测试数据：合成和成养老区、1栋/2栋、3楼/5楼/2楼、301/512/218 房间。
- 外部依赖：后端测试使用 H2 PostgreSQL mode + Flyway；开发联调使用 Docker PostgreSQL。

## 3. 用例清单

| ID | 类型 | 场景 | 前置条件 | 步骤 | 期望结果 | 自动化 | 优先级 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TC-001 | Unit | 和成养老唯一活动区 | 房间目录初始化 | 校验 active zones | 只有一个活动区且名称为和成养老 | 是 | P0 |
| TC-002 | Unit | 居住地点格式化 | 存在 301 房间 | 生成 locationLabel | 输出“和成养老 - 1栋 - 3楼 - 301号房” | 是 | P0 |
| TC-003 | Unit | 按楼栋查询房间 | 存在 1栋房间 | 查询 building-1 | 返回 301、512 | 是 | P0 |
| TC-004 | Unit | 可入住房间判断 | 房间容量和占用数存在 | 判断 301 和 512 | 301 可住，512 满房不可住 | 是 | P0 |
| TC-005 | Unit | 长者档案引用房间 | 长者档案使用 roomId | 生成 ResidentView | room 展示完整区-栋-楼-房 | 是 | P0 |
| TC-006 | Contract | 非和成养老区请求 | 后端 API 接入 | 传入其他 zoneId | 返回 ROOM_ZONE_SCOPE_INVALID | 部分 | P0 |
| TC-007 | Integration | 重复房间防重 | 后端 DB 接入 | 同楼层重复创建 301 | 返回 ROOM_DUPLICATED | 是 | P0 |
| TC-007B | Integration | 修改为重复房号防重 | 已存在 301 房间 | 将其他房间改为同楼层 301 | 返回 ROOM_DUPLICATED | 是 | P0 |
| TC-008 | Integration | 床位占用事务 | 后端锁/事务接入 | 分配长者到 218 房 A 床 | 更新房间占用数、房态和长者档案 roomId | 是 | P0 |
| TC-009 | Security | 越权维护房间 | 社工角色 | 调用创建/修改 API | 返回 ROOM_FORBIDDEN | 是 | P0 |
| TC-010 | Audit | 关键操作审计 | 创建/修改/停用/床位分配 | 调用写接口 | 写入 audit_logs | 是 | P1 |
| TC-011 | Security | 缺少认证 | 不提供 Bearer token | 调用房间树 API | 返回 AUTH_REQUIRED | 是 | P0 |
| TC-012 | Integration | 幂等创建 | 提供 Idempotency-Key | 重放相同创建请求 | 返回同一房间资源 | 是 | P0 |

## 4. 执行记录

| 时间 | 环境 | 命令/方式 | 结果 | 失败摘要 | 修复记录 |
| --- | --- | --- | --- | --- | --- |
| 2026-04-26 | local | `npm test` | Pass | 首次失败：长者档案机构过滤仍使用旧 `facility-east` | 改为 `facility-hecheng` 后复跑通过；shared-domain 10/10，web-admin 3/3 |
| 2026-04-26 | local | `npm run build` | Pass | 无阻塞失败 | 构建通过；仍有 Ant Design chunk size warning |
| 2026-04-26 | local | `cd apps/backend && mvn test` | Pass | 无 | 后端房间管理 Controller/Service/Flyway 集成测试 9/9 通过 |
| 2026-04-26 | local | `cd apps/backend && mvn package` | Pass | 无 | 后端测试 9/9 通过并完成 Spring Boot jar 打包 |
| 2026-04-26 | local | `docker compose -f docker/dev/docker-compose.yml --profile backend config` | Pass | 无 | 开发环境 PostgreSQL + backend Compose 配置可解析 |
| 2026-04-26 | local | `npm test` | Pass | 无阻塞失败；Web 测试仍有 Ant Design `act(...)` 警告 | shared-domain 10/10，web-admin 3/3 |
| 2026-04-26 | local | `bash -n scripts/start-dev.sh scripts/start-web.sh scripts/start-backend-dev.sh scripts/start-backend-docker.sh` | Pass | 无 | 一键启动脚本语法检查通过 |
| 2026-04-26 | local | `npm run test:scripts` | Pass | 无 | 启动脚本 dry-run 覆盖全栈启动、后端本机启动、后端 Docker 启动、Web 单独启动；验证自定义端口透传 |

## 5. 回归结论

- 已通过：和成养老唯一活动区、区-栋-楼-房格式化、层级查询、可入住房间判断、长者档案房间引用、后端房间树、可住房间查询、重复房间防重、层级不一致、认证必填、角色维护越权、幂等创建、床位分配同步长者档案、Web 组件测试、生产构建。
- 未通过：无。
- 跳过项及原因：多线程并发压测、生产身份源和审计检索界面尚未实现。
- 剩余风险：生产部署策略、生产身份源和密钥管理仍需后续 ADR 固化。
