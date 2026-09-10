# 提案：账务权限管理系统（Ledger Admin System）

## Why

实习生 AI Coding 全栈开发实战考核要求在 2 周内交付一套"后台权限管理 + 财务收支记账"企业级系统，80% 以上代码须由 AI 生成，且 OpenSpec 文档本身就是交付物之一。本 change 将该考核需求形式化为规范的 AI 开发基线，作为后续编码、答辩与评分的唯一事实来源。

## What Changes

- 新建前端工程 `frontend/`：Vue3 + Vite + TypeScript + Element Plus + Pinia + Axios，实现登录页、动态侧边栏布局、用户/角色/菜单管理页、收支分类/记录/统计页、数据看板。
- 新建后端工程 `backend/`：Spring Boot 3 + MyBatis-Plus + MySQL 8 + JWT，实现认证、RBAC、记账业务与统计/导出接口。
- 新增数据库设计：`sys_user / sys_role / sys_menu / sys_user_role / sys_role_menu / fin_category / fin_record / sys_oper_log` 共 8 张表。
- 完整实现基础必做（80 分）：登录认证、用户管理、角色管理、菜单管理、收支分类、收支记录、多条件筛选、金额统计。
- 实现全部可选加分项（20 分）：数据可视化看板、Excel 导出、批量操作、按钮级权限、软删除与操作日志。
- 新增交付文档规划：工程介绍文档与 AI 提示词记录存放于 `docs/`。

## Capabilities

### New Capabilities

- `auth`: 账号密码登录、JWT 签发/校验/过期、前端路由守卫与密码加密存储。
- `rbac`: 用户、角色、菜单的 CRUD 与管理，角色-菜单权限分配，前端动态菜单渲染，按钮级权限控制。
- `finance`: 收支分类管理、收支记录 CRUD、多条件筛选查询、基础金额统计。
- `dashboard`: 基于 ECharts 的数据看板（近 12 月收支趋势、分类占比、近 7 日收支对比）。
- `data-export`: 收支记录 Excel 导出（筛选/全量/批量）与批量删除操作。
- `audit`: 业务数据软删除标记与操作日志自动记录（可追溯、可审计）。

### Modified Capabilities

无（全新系统，无既有 spec）。

## Impact

- 代码：`frontend/`、`backend/` 两个全新工程（当前工作区无既有代码）。
- 依赖：Node 24 / npm、JDK 25（`C:\Program Files\Java\jdk-25`）、Maven（`D:\maven`）、MySQL 8.0.34（本机服务）、jjwt、MyBatis-Plus、EasyExcel、ECharts。
- 数据：新增 8 张表，金额字段使用 `DECIMAL(12,2)` 保证精度。
- 文档：`openspec/` 新增本 change 的 specs/design/tasks；`docs/` 预留交付文档位置。
- 非目标：不引入 Redis；不做移动端/小程序；不做多租户；不部署上线（仅本地可运行）。
