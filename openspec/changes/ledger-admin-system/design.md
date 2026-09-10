# 设计文档：账务权限管理系统（Ledger Admin System）

## Context

考核需求与动机见 `proposal.md`。本系统为全新工程，当前工作区无既有代码。本机环境约束已核实：

- Node 24 / npm 11（前端）、JDK 25 于 `C:\Program Files\Java\jdk-25`、Maven 于 `D:\maven`、MySQL 8.0.34 本机服务、Redis 5.0.14（本轮不引入）。
- 全局 `java` 默认指向 Java 8，构建时必须显式使用 JDK 25。

## Goals / Non-Goals

**Goals:**

- 两周内交付可运行的前后端完整工程，覆盖 80 分基础功能与 20 分全部加分项。
- 以 OpenSpec 作为 AI 开发的规范基线，确保 AI 生成的 80% 代码行为一致、可验收。
- 架构清晰、可演示、可答辩：分层、统一返回、权限隔离严格生效。

**Non-Goals:**

- 不引入 Redis/缓存层、不做多租户、不做移动端、不做线上部署。
- 不引入微服务/消息队列等与企业中后台不符的复杂度。
- 金额以外的业务不做多币种、不做报表导出（仅 Excel）。

## Decisions

### D1: 前端选型 Vue3 + Vite + TypeScript + Element Plus + Pinia

- 选择理由：考核推荐 Vue3 + Vite；Element Plus 是国内中后台事实标准，AI 语料最丰富；TS 提升健壮性（考核鼓励项）。
- 备选：Vue2 + Vuex（考核允许但已过时）、Naive UI / Ant Design Vue（功能近似，语料与模板较少）。
- HTTP 统一用 Axios 封装（`request.ts`），拦截器注入 `Authorization: Bearer <token>` 并统一处理 401/403。

### D2: 后端 Spring Boot 3.5 + JDK 25 + Maven

- 选择理由：用户 Java/Spring Boot 熟练度最高；SB3 + MyBatis-Plus 3.5 对 CRUD/分页/逻辑删除开箱即用。
- 备选：FastAPI（Python 3.12 现成但偏离熟练度）、Spring Boot 2.7 + Java 8（已 EOL）。
- 构建约束：使用 `D:\maven\bin\mvn.cmd`，并在命令/IDE 中把 JDK 切到 jdk-25（`maven.compiler.release=21`）。

### D3: ORM 使用 MyBatis-Plus（spring-boot3 starter）

- 选择理由：内置分页插件、逻辑删除、代码生成，Mapper XML 便于书写统计聚合 SQL（按月/按日/分类占比）。
- 备选：Spring Data JPA（Hibernate，动态条件查询与复杂统计 SQL 不如 MyBatis 直观）。
- 关键配置：`logic-delete-field: is_deleted` 全局逻辑删除，分页拦截器 `PaginationInnerInterceptor`。

### D4: 认证用 JWT（jjwt 0.12）+ 无状态会话

- 选择理由：考核硬性要求 JWT；jjwt 轻量、无框架绑定，便于讲解实现细节（答辩抽问友好）。
- 方案：登录成功后签发 HS256 令牌（含 userId/username，过期时间 2h）；`JwtAuthFilter` 在 Spring Security 过滤器链中解析并注入 `LoginUser`。
- 备选：Spring Security OAuth2 Resource Server（配置复杂、讲解成本高）、Sa-Token（非考核指定）。
- 安全：密码用 BCrypt 哈希；统一放行 `/api/auth/login`、静态资源，其余接口鉴权；401/403 统一 JSON 返回。
- URL 约定：所有后端接口统一前缀 `/api`，详见 D10。

### D5: RBAC 五表模型 + 菜单/按钮权限

- 表：`sys_user / sys_role / sys_menu / sys_user_role / sys_role_menu`。
- `sys_menu` 含 `menu_type`（1 目录 / 2 菜单 / 3 按钮）与 `perms`（如 `finance:record:add`），按钮权限走 perms 匹配。
- 登录后接口返回当前用户的菜单树（含按钮 perms 列表），前端据此渲染动态侧边栏 + `v-permission` 指令控制按钮显隐。
- 路由策略：前端**静态注册**全部合法页面路由，在路由元信息 `meta.perm` 和 `meta.requiresAuth` 上做守卫，侧边栏菜单从后端动态渲染；避免动态添加路由的刷新闪烁。
- 按钮级后端校验：使用自定义表达式 `@PreAuthorize("@ss.hasPermi('{perms}')")`，从当前用户 perms 列表判定。

### D6: 金额精度 DECIMAL(12,2) + BigDecimal

- 选择理由：考核明确"金额计算精准无误差"，浮点 double 必然踩坑。
- 实现：MySQL `DECIMAL(12,2)`，Java 实体统一 `BigDecimal`，统计聚合 SQL 用 `SUM(amount)`，前端展示保留两位。

### D7: 数据隔离按用户

- 普通用户只读写本人 `fin_record`（SQL 强制带 `user_id = 当前用户`）。
- 管理员判定：角色编码为 `admin` 的角色视为管理员，可查看与统计全部记录；数据隔离在列表、统计、导出、看板接口中统一生效。

### D8: 加分项落地方案

| 加分项 | 方案 |
|---|---|
| 4.1 数据看板 | ECharts + 后端 3 个统计接口（近12月按月 SUM、近7日按日 SUM、分类占比 GROUP BY category）；数据范围遵循 D7 隔离规则 |
| 4.2 Excel 导出 | EasyExcel 4.x，导出接口复用筛选条件 DTO，全量/筛选/批量共用一个导出入口；导出内容不包含已软删除数据 |
| 4.3 批量操作 | 前端表格多选（selection）→ 后端批量删除（ids + 逻辑删除）、批量导出接口 |
| 4.4 按钮级权限 | `sys_menu` 按钮节点 + `perms` 字段 + 前端 `v-permission` 指令 + 后端 `@PreAuthorize("@ss.hasPermi('{perms}')")` 校验 |
| 4.5 软删除+日志 | MyBatis-Plus 全局逻辑删除 + `@OperLog` 注解 + Spring AOP 切面写 `sys_oper_log`；日志字段含模块、操作类型、请求方法、URL、参数(JSON 前512字符)、结果、错误信息、IP、耗时 |

### D10: 接口 URL 统一约定

| 模块 | 前缀 | 示例 |
|---|---|---|
| 认证 | `/api/auth` | `POST /api/auth/login`、`GET /api/auth/info` |
| 系统管理（RBAC） | `/api/system` | `/api/system/users`、`/api/system/roles`、`/api/system/menus`、`GET /api/system/menus/tree` |
| 财务 | `/api/finance` | `/api/finance/categories`、`/api/finance/records` |
| 看板 | `/api/dashboard` | `GET /api/dashboard/stats` |
| 导出 | `/api/export` | `POST /api/export/records` |
| 审计日志 | `/api/audit` | `GET /api/audit/logs` |

Vite 开发服务器代理 `/api/*` 到后端 `http://localhost:8080`。

### D9: 工程结构

Monorepo 双工程（`frontend/` + `backend/`）+ `docs/`（工程介绍、提示词记录）+ `openspec/`。后端按 `config/security/controller/service/mapper/entity/dto/common/aspect` 分包，前端按 `api/router/stores/layouts/views/components/directives/utils` 分包。详见 proposal 中"Impact"与上一轮确认的目录树。

## Risks / Trade-offs

- **JDK 版本混乱**（全局 java 是 8）→ 构建脚本/文档中固定 `JAVA_HOME=C:\Program Files\Java\jdk-25`；Maven 用 `maven.compiler.release=21` 强制编译目标。
- **两周期限紧张** → 实施顺序：先工程骨架+数据库 → 认证/RBAC（前端+后端）→ 财务模块 → 加分项 → 文档/答辩材料；加分项按 6/4/4/3/3 分值排序投入。
- **AI 生成代码质量不可控** → 以 OpenSpec specs 作为验收契约，任务逐个模块走"AI 生成 → 自测 → 修复"闭环；关键安全点（密码、JWT、权限隔离）人工复核。
- **MyBatis-Plus 与 SB3 兼容** → 使用 `mybatis-plus-spring-boot3-starter`（3.5.x），避免用旧 `mybatis-plus-boot-starter`。
- **金额精度** → 全链路 BigDecimal + DECIMAL，禁止 double；统计 SQL 用 SUM 聚合。
- **评分口径矛盾**（基础子项小分与总分对不上）→ 按"全部功能做完整"原则实现，答辩前与评委确认口径。

## Migration Plan

- 本地开发与演示，无线上部署。初始化：`backend/src/main/resources/sql/schema.sql + data.sql`（建库建表 + 默认管理员/角色/菜单种子数据），启动时或手动执行。
- 默认种子账号：
  - 管理员 `admin / admin123`：角色编码 `admin`，拥有全部菜单与按钮权限。
  - 普通用户 `user / user123`：角色编码 `user`，仅拥有财务模块菜单与按钮权限。
- 前端 `npm install && npm run dev`（Vite 代理 `/api` 到后端 8080）；后端 `mvn spring-boot:run`。
- 回滚：无既有系统，Git 管理即可。

## Open Questions

- 验收评分口径（基础子项合计 50 vs 总分 80；补充维度 30 vs 50）需与评委确认——不影响本设计与实施顺序，仅影响答辩侧重点。
