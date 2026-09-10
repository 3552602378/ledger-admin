# 实施任务：账务权限管理系统（Ledger Admin System）

> 顺序执行；每个模块内建议后端接口 → 前端页面的顺序推进。

## 1. 工程初始化与环境

- [x] 1.1 创建后端工程骨架（Maven 项目，JDK 指向 `C:\Program Files\Java\jdk-25`，`maven.compiler.release=21`），引入 `spring-boot-starter-parent 3.5.x`、`mybatis-plus-spring-boot3-starter`、`mysql-connector-j`、`jjwt-impl`、`jjwt-jackson`、`easyexcel`、`spring-boot-starter-validation`、`spring-boot-starter-aop`、`lombok` 依赖，验证 `D:\maven\bin\mvn.cmd compile` 成功
- [x] 1.2 创建前端工程骨架（Vite + Vue3 + TypeScript + Element Plus + Pinia + Axios + ECharts），配置 Vite 开发代理 `/api/*` → `http://localhost:8081`，验证 `npm install` 与 `npm run dev` 可启动
- [x] 1.3 编写数据库初始化脚本 `backend/src/main/resources/sql/schema.sql + data.sql`，建库 `ledger` 编码 utf8mb4，创建 8 张表（`sys_user / sys_role / sys_menu / sys_user_role / sys_role_menu / fin_category / fin_record / sys_oper_log`），并写入种子数据：管理员角色编码 `admin`、普通用户角色编码 `user`、两个账号 `admin/admin123` 与 `user/user123`（密码由后端 InitDataRunner 生成），管理员拥有全部菜单/按钮权限，普通用户仅拥有财务模块权限
- [x] 1.4 实现后端通用层：统一返回类 `Result<T>` 与 `PageResult<T>`、全局异常处理 `GlobalExceptionHandler`、MyBatis-Plus 分页插件 `PaginationInnerInterceptor`、全局逻辑删除字段 `is_deleted`、跨域配置 `CorsConfig`，验证 Spring Boot 应用启动日志无 ERROR
- [x] 1.5 实现前端基础层：axios 封装 `request.ts`（自动注入 `Authorization: Bearer <token>`，统一处理 401/403）、Pinia store 骨架（`user.ts`、`permission.ts`）、基础主布局 `Layout.vue`、登录页 `LoginView.vue`，验证登录页 `http://localhost:5173/login` 可渲染

## 2. 认证模块（auth）

- [ ] 2.1 实现后端登录接口 `POST /api/auth/login`：校验账号状态 → BCrypt 校验密码 → jjwt 0.12 签发 HS256 令牌（payload 含 `userId`、`username`，过期 2 小时）→ 返回 token 与当前用户基础信息；错误时返回 401 与"用户名或密码错误"，不泄露账号存在性；验证正确/错误凭据返回符合预期
- [ ] 2.2 实现 Spring Security 配置与 JWT 过滤器：自定义 `JwtAuthenticationFilter` 解析令牌并注入 `LoginUser`；放行 `/api/auth/login`、静态资源；其余请求须携带有效 token；未登录返回 401，无权限返回 403；统一 JSON 响应格式；验证无 token 访问 `/api/system/users` 返回 401
- [ ] 2.3 实现前端认证闭环：登录成功后本地存储 token 到 `localStorage`；请求拦截器自动注入 token；401 时清除 token 并跳转登录页；Vue Router 全局守卫在未登录时重定向到 `/login`，已登录访问 `/login` 重定向到首页；验证登录成功跳转 `/dashboard`、token 过期后访问受保护页面自动登出

## 3. 基础管理模块（RBAC）

- [ ] 3.1 实现后端用户管理接口（`/api/system/users`）：分页列表（关键词搜索用户名/昵称）、新增（密码 BCrypt 加密）、编辑、启用/禁用、重置密码（生成新密码并 BCrypt 加密）；用户表密码字段永远不在响应中返回；验证各接口 JSON 输出与数据库变更
- [ ] 3.2 实现后端角色管理接口（`/api/system/roles`）：角色 CRUD + 角色-菜单关联保存/查询；删除角色时级联删除 `sys_user_role` 与 `sys_role_menu` 关联；验证分配菜单后关联表正确
- [ ] 3.3 实现后端菜单管理接口（`/api/system/menus`）：菜单 CRUD、树形查询、排序、菜单类型字段 `menu_type`（1 目录 / 2 菜单 / 3 按钮）、`perms` 权限标识；删除含子节点的菜单时拒绝或提示；验证树形接口返回层级正确
- [ ] 3.4 实现后端当前用户权限接口 `GET /api/auth/info`：返回当前用户信息、菜单树、按钮 `perms` 列表；验证 `admin` 与 `user` 登录后返回的菜单与 perms 不同
- [ ] 3.5 实现前端用户管理页（`/system/users`）：Element Plus 表格分页、关键词搜索、新增/编辑对话框、启用禁用开关、重置密码按钮；页面加载调 `/api/system/users` 并正确渲染
- [ ] 3.6 实现前端角色管理页（`/system/roles`）：表格列表、新增/编辑/删除、权限分配抽屉（Element Plus 树形选择菜单节点，含按钮节点）、保存时提交 `menuIds`；验证保存后该角色用户重新登录菜单变化
- [ ] 3.7 实现前端菜单管理页（`/system/menus`）：树形表格展示、新增/编辑/删除、排序字段编辑；验证对菜单的增删改即时生效
- [ ] 3.8 实现前端动态侧边栏与路由守卫：侧边栏根据 `/api/auth/info` 返回的菜单树递归渲染；路由元信息 `meta.perm` 做越权访问拦截；验证 `admin` 登录显示全部菜单、`user` 登录只显示财务菜单、直接访问 `/system/users` 被拦截

## 4. 财务模块（finance）

- [ ] 4.1 实现后端收支分类接口（`/api/finance/categories`）：字段 `type`（income/expense）、名称 CRUD；删除前校验是否被 `fin_record` 引用，若被引用则拒绝删除并提示；实现前端分类管理页；验证增删改查与引用校验
- [ ] 4.2 实现后端收支记录接口（`/api/finance/records`）：新增记录时自动将 `user_id` 设为当前登录用户 ID；编辑、删除（逻辑删除）、分页列表；普通用户只能操作/查看本人记录，管理员可操作/查看全部；验证数据归属正确
- [ ] 4.3 实现后端多条件筛选接口：支持时间维度（`today/week/month/custom` + 起止日期）、类型（income/expense）、分类 ID、备注关键词模糊搜索、分页；参数为空时不参与筛选；验证组合条件结果与 SQL 一致
- [ ] 4.4 实现后端基础金额统计接口（`/api/finance/records/statistics`）：按当前筛选条件返回 `totalIncome / totalExpense / balance`，后端使用 `BigDecimal` 与 `SUM(amount)` 计算；验证含小数金额时精度精确到分、切换筛选条件后统计联动
- [ ] 4.5 实现前端收支记录页（`/finance/records`）：顶部筛选栏（快捷时间按钮 + 日期范围选择器 + 类型下拉 + 分类下拉 + 备注搜索）、统计卡片（收入/支出/结余）、新增/编辑抽屉、分页表格；验证新增记录后列表与统计同步刷新

## 5. 进阶加分项（bonus）

- [ ] 5.1 实现看板统计接口 `GET /api/dashboard/stats`：返回近 12 个月按月收入支出、近 7 日按日收入支出、分类金额占比三组数据；数据范围遵循 D7 隔离规则（管理员全部，普通用户本人）；前端 Dashboard 页使用 ECharts 渲染折线图/饼图/柱状图；验证新增记录后图表刷新
- [ ] 5.2 实现 Excel 导出接口 `POST /api/export/records`：使用 EasyExcel 4.x，参数复用记录筛选 DTO 并增加 `ids` 数组与 `exportAll` 标志；`exportAll=true` 导出全量（不含已软删除），`ids` 非空导出选中记录，否则导出当前筛选结果；验证下载文件无乱码、金额保留两位、数据与列表一致
- [ ] 5.3 实现前端批量操作：收支记录表格支持多选（el-table selection-column），顶部显示"批量删除"与"批量导出"按钮；批量删除调用 `DELETE /api/finance/records/batch`（请求体 ids 数组 + 逻辑删除）；批量导出调用 `/api/export/records` 并触发下载；验证选中记录批量生效
- [ ] 5.4 实现按钮级权限控制：在 `sys_menu` 中新增按钮节点（如 `system:user:add`、`finance:record:export`）；前端注册 `v-permission` 指令，根据当前用户 perms 列表显隐按钮；后端 Controller 方法加 `@PreAuthorize("@ss.hasPermi('system:user:add')")`；验证无权限时按钮隐藏且直接调接口返回 403
- [ ] 5.5 验证数据软删除生效：确认 `fin_record` 等表存在 `is_deleted` 字段且 MyBatis-Plus 全局逻辑删除已开启；删除记录后数据库 `is_deleted=1`；列表、统计、导出、看板均不统计已删除记录；验证软删除行为全局一致
- [ ] 5.6 实现操作日志：创建 `@OperLog(title, businessType)` 注解与 AOP 切面 `OperLogAspect`；在关键 Controller 方法上标注注解；切面记录模块名、业务类型（新增/修改/删除/导出等）、请求方法、URL、请求参数（JSON 前 512 字符）、操作结果、错误信息、IP、耗时，写入 `sys_oper_log`；新增操作日志查询页 `/audit/logs`；验证增删改操作均产生日志，失败操作也记录

## 6. 验收与交付文档

- [ ] 6.1 前后端联调自测：按 `openspec/changes/ledger-admin-system/specs/` 下全部 scenarios 逐条执行，记录测试结果与截图到 `docs/test-report.md`，验证全部通过
- [ ] 6.2 编写工程介绍文档 `docs/README.md`：包含项目概述、技术选型、架构图（文字描述或 ASCII）、数据库表说明、后端/前端目录说明、启动步骤（JDK/Maven/Node/MySQL 环境配置、脚本执行、启动命令）、默认账号；验证文档与实现一致
- [ ] 6.3 整理 AI 提示词记录 `docs/prompts.md`：按模块分类记录引导 AI 完成核心功能的关键 Prompt（认证、RBAC、财务、看板、导出、日志），并附简短反思；验证记录完整、可读
- [ ] 6.4 编写答辩演示脚本 `docs/presentation.md`：设计 10–15 分钟演示流程（工程介绍 → 登录/RBAC 演示 → 财务模块演示 → 看板/导出/批量/日志加分项 → OpenSpec 文档讲解 → 收尾）；验证可在 20 分钟内完整走查
