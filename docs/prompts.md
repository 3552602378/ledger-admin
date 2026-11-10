# AI 提示词记录

> 本文档按模块记录开发过程中用于引导 AI 完成核心功能的关键 Prompt，并附简短反思。
> 记录的是"输入 → 期望产出 → 实际效果"的对应关系，便于复现与评估 AI 协作方式。
> 开发全程以 `openspec/changes/ledger-admin-system/` 的 spec 作为验收契约，Prompt 多以"引用 spec 编号 + 约束条件"的方式组织。

## 使用方式说明

整体策略分三步：

1. **先规范后编码**：先用 OpenSpec 把需求固化成 proposal / design / tasks / specs，再让 AI 按 spec 实现，避免"边想边写"导致返工。
2. **一次只做一个模块**：每个 Prompt 对应 tasks.md 中的一个编号任务，产出后立即自测并提交，保证可回滚。
3. **约束写在 Prompt 里**：把技术栈版本、安全要求、数据隔离规则、禁止事项（如"禁用 double"）显式写进提示词，比事后纠正成本低。

---

## 一、工程骨架与数据库

### P1 后端工程初始化

```
按 openspec/changes/ledger-admin-system/design.md 的 D2、D3 决策，创建 Spring Boot 3.5 Maven 工程：
- parent 用 spring-boot-starter-parent 3.5.0，maven.compiler.release=21
- 依赖：web、security、validation、aop、mysql-connector-j、
  mybatis-plus-spring-boot3-starter 3.5.12（注意必须是 spring-boot3 版本，不是旧的 boot-starter）、
  mybatis-plus-jsqlparser、jjwt-api/impl/jackson 0.12.6、easyexcel 4.0.3、lombok
- 包结构按 config/security/controller/service/mapper/entity/dto/common/aspect/annotation 分包
- 本机全局 java 是 8，构建时必须用 JDK 25，构建命令要显式设置 JAVA_HOME
```

**产出**：`pom.xml` + `LedgerApplication.java` + 空包结构。

**反思**：把"必须是 spring-boot3 starter"写进 Prompt 很关键。MyBatis-Plus 有两套 starter，旧的 `mybatis-plus-boot-starter` 在 Boot 3 下会因 `javax` 与 `jakarta` 冲突启动失败，事后排查耗时更长。另外 `mybatis-plus-jsqlparser` 在 3.5.9+ 是独立依赖，不显式引入会导致分页插件报错。

### P2 数据库脚本

```
按 design.md D5、D6、D7 生成 schema.sql 与 data.sql：
- 8 张表：sys_user/sys_role/sys_menu/sys_user_role/sys_role_menu/fin_category/fin_record/sys_oper_log
- 金额字段用 DECIMAL(12,2)，禁止 float/double
- 业务表都要有 is_deleted TINYINT DEFAULT 0
- fin_record 加 (user_id, record_date) 联合索引与 type 索引
- data.sql 写角色（admin/user）、菜单树（含 menu_type 1/2/3 与 perms）、角色菜单关联、示例分类
- 用户账号不要写死在 SQL 里，由后端 InitDataRunner 用 BCrypt 生成，避免明文哈希进版本库
```

**产出**：`schema.sql`（116 行）、`data.sql`（60 行）、`InitDataRunner.java`。

**反思**：一开始让 AI 直接把 BCrypt 哈希写进 data.sql，导致每次改密码都要重新算哈希，且哈希值在代码评审时无法验证。改成启动时初始化后，密码规则集中在 Java 代码里，种子数据更干净。

---

## 二、认证模块

### P3 JWT 登录与过滤器

```
实现 spec auth 的 Requirement「账号密码登录」「JWT 令牌校验」，对应 tasks 2.1、2.2：
- POST /api/auth/login，用 AuthenticationManager + DaoAuthenticationProvider 校验
- 密码 BCrypt，用户名不存在与密码错误都返回同一句「用户名或密码错误」，不泄露账号存在性
- 禁用账号要返回明确提示「账号已被禁用」，不要落进通用异常处理返回 500
- jjwt 0.12 写法：Jwts.builder().claims().subject().issuedAt().expiration().signWith(key, Jwts.SIG.HS256)
- JwtAuthenticationFilter 继承 OncePerRequestFilter，解析失败只记日志不抛异常，让后续链路的 EntryPoint 返回 401
- SecurityConfig：仅放行 /api/auth/login 与 OPTIONS，其余 authenticated()，csrf 关闭，session STATELESS
- 未认证用 AuthenticationEntryPoint 返回 401 JSON，无权限用 AccessDeniedHandler 返回 403 JSON
```

**产出**：`AuthController`、`AuthService`、`JwtTokenUtil`、`JwtAuthenticationFilter`、`SecurityConfig`、两个 Handler。

**反思**：`DisabledException` 是这次踩到的坑。Spring Security 的 `DaoAuthenticationProvider` 在账号被禁用时抛的是 `DisabledException`，如果不单独 catch，会被全局 `Exception` 处理器接住返回 500「系统繁忙」，用户体验很差。这一点必须写进 Prompt，AI 不会主动想到。

### P4 前端认证闭环

```
实现 spec auth 的「前端路由守卫」，对应 tasks 2.3：
- utils/request.ts：请求拦截器从 localStorage 注入 Authorization: Bearer <token>
- 响应拦截器：401 清 token 并跳 /login；其余错误统一 ElMessage 提示
- router/index.ts：beforeEach 里判断 meta.public 与 isLoggedIn，未登录跳 /login，已登录访问 /login 跳首页
- 首次进入受保护路由且 menus 为空时先 fetchUserInfo，失败则清 token 回登录页
```

**反思**：路由守卫里"先拉权限再放行"这一步容易写成死循环（跳转 → 守卫 → 再跳转）。后来按 spec 的"静态注册路由"方案，越权时跳到用户可访问的首个菜单而非被拦目标，循环问题才彻底解决。

---

## 三、RBAC 模块

### P5 用户 / 角色 / 菜单后端

```
实现 spec rbac 的「用户管理」「角色管理」「菜单管理」，对应 tasks 3.1–3.3：
- 用户：分页 + 关键词搜 username/nickname、新增（BCrypt）、编辑、启用禁用、重置密码
- 用户响应里永远不能出现 password 字段（实体上加 @JsonIgnore）
- 新增/编辑用户要能分配角色，UserVO 需要带 roleId 供前端回显
- 角色：CRUD + 角色菜单关联保存/查询；删除角色级联清理 sys_user_role 与 sys_role_menu
- 角色菜单保存：menuIds 为 null 表示"本次不改动权限"，不要清空；只有显式传数组（含空数组）才覆盖
- 菜单：树形查询、CRUD、menu_type 区分目录/菜单/按钮、删除有子节点时拒绝
- 每个接口加 @PreAuthorize("@ss.hasPermi('xxx')")，perms 与 data.sql 中 sys_menu.perms 一致
```

**产出**：3 个 Controller、3 个 Service、DTO 若干。

**反思**：`menuIds` 的语义是这次最重要的教训。最初实现是"传了数组就删除重建"，前端编辑角色的基本信息时习惯性把 `menuIds: []` 一起提交，结果保存一次就把该角色的全部菜单权限清空了 —— 这是个会丢数据的设计缺陷。修复方案是在 Prompt 里明确区分"null = 不改动"与"数组 = 覆盖"，前端基本信息表单不再提交该字段。**这类"可选字段的 null 与空数组语义"必须在设计阶段就定清楚。**

### P6 权限接口与动态菜单

```
实现 spec rbac 的「前端动态菜单渲染」，对应 tasks 3.4、3.8：
- GET /api/auth/info 返回 { user, menus, perms, admin }
- menus 是树形，排除 menu_type=3 的按钮节点；perms 是扁平字符串数组（含按钮）
- 管理员（角色编码 admin）返回 *:*:* 通配，并直接取全部启用菜单
- 非管理员按 sys_user_role → sys_role_menu → sys_menu 查，且只取 status=1 的菜单
- 前端 SubMenu.vue 递归渲染 el-sub-menu / el-menu-item，菜单数据完全来自后端
- 路由静态注册，meta.perm 声明所需权限；守卫里 perms 不含该值就跳首页
```

**反思**：非管理员分支一开始用 `selectBatchIds` 取菜单，忘了过滤 `status`，导致管理员禁用的菜单在普通用户侧边栏仍然出现。这类"绕过 wrapper 直接按 id 查"的写法容易漏掉状态过滤，需要显式约束。

### P7 按钮级权限

```
实现 spec rbac 的「按钮级权限控制」，对应 tasks 5.4：
- 前端：注册 v-permission 指令（已注册）+ 提供 hasPerm() 组合式判断
- 后端：Controller 方法加 @PreAuthorize("@ss.hasPermi('finance:record:export')")
- PermissionService 注册为 Bean 名 ss，实现 hasPermi，支持 *:*:* 通配
- 验收标准：无权限时按钮不渲染，且直接调接口返回 403
```

**反思**：实际落地时统一用了 `hasPerm()` 函数而没使用指令，因为指令在 `v-if` 混合场景下可读性差，而函数式判断在模板里更直观。功能等价，但 Prompt 里写的"注册 v-permission 指令"与实际用法有偏差 —— 说明提示词描述实现手段时，不如描述验收标准（"按钮不渲染 + 接口 403"）来得稳。

---

## 四、财务模块

### P8 收支记录与数据隔离

```
实现 spec finance 的「收支记录管理」「数据归属隔离」，对应 tasks 4.2：
- 新增记录时 user_id 强制取当前登录用户，忽略前端传入值
- 编辑/删除前校验记录存在且归属当前用户（管理员例外）
- 所有删除走逻辑删除，由 MyBatis-Plus @TableLogic 自动处理
- 隔离规则写成一个 isolationWrapper() 方法，列表/统计/导出/看板四个入口共用
- 判断管理员：角色编码为 admin，不是靠 userId==1 硬编码
```

**反思**：把隔离规则抽成一个方法复用，比在四个地方各写一遍 `if (!isAdmin) wrapper.eq("user_id", ...)` 可靠得多。看板接口用 `userId=null` 表示管理员不追加条件，这个约定也要写进 Prompt，否则 AI 容易用 `userId=0` 之类的魔法值。

### P9 多条件筛选与统计

```
实现 spec finance 的「多条件筛选查询」「基础金额统计」，对应 tasks 4.3、4.4：
- 时间维度 dateType：today / week（自然周，周一至今天）/ month（本月 1 号至今）/ custom（起止日期）
- 类型 income/expense、分类 categoryId、备注 remark 模糊
- 参数为空时不参与筛选，不要拼出 where 1=1 之类的冗余条件
- 统计接口返回 totalIncome / totalExpense / balance，用 SUM(CASE WHEN type='income' ...)
- 统计的筛选条件必须与列表完全一致（含类型），保证"切换筛选统计联动"
- 全程 BigDecimal，统计结果 setScale(2)，禁止任何 double 参与计算
```

**反思**：早期实现里统计刻意忽略 `type` 参数，本意是"让收入和支出两个数字同时可见"，但这违反了 spec 的「统计随筛选条件实时更新」。改为完全跟随筛选后，选"支出"时收入自然显示 0，语义更一致，也和答辩时的演示口径吻合。**spec 怎么写就怎么实现，不要自作聪明加"更友好"的行为。**

### P10 前端记录页

```
实现 tasks 4.5 的前端收支记录页：
- 顶部统计卡片：收入（绿色）/ 支出（红色）/ 结余（蓝色渐变）
- 筛选栏：快捷时间 radio（全部/今天/本周/本月/自定义）+ 日期范围 + 类型下拉 + 分类下拉 + 备注输入
- 抽屉新增/编辑：类型切换时清空已选分类，避免选了收入却绑定支出分类
- 分类下拉按当前类型过滤，筛选栏与抽屉各自独立过滤
- 表格多选 + 分页；新增/编辑后列表与统计同时刷新
```

**反思**：抽屉里的分类过滤最初和顶部筛选栏共用了 `filter.type`，导致在筛选栏选了"支出"后，抽屉里新增收入时分类选项也只剩支出分类。改成抽屉独立读 `form.type` 才正确。**同名字段在筛选态与编辑态混用是常见 bug 源。**

---

## 五、进阶加分项

### P11 数据看板

```
实现 spec dashboard，对应 tasks 5.1：
- GET /api/dashboard/stats 返回三组数据：近 12 月按月收入支出、近 7 日按日收入支出、分类金额占比
- 用 MyBatis @Select 注解写聚合 SQL，DATE_FORMAT 分组；SQL 里显式写 is_deleted = 0
- 管理员 userId 传 null 不追加用户条件，普通用户传自己的 id
- 月份/日期序列要在 Java 里补齐空档（没有数据的月份返回 0），不能让前端处理缺失
- 前端 ECharts：折线（12 月）、折线（7 日）、环形饼图（分类占比），窗口 resize 时同步 resize
```

**反思**：补齐空档这一步必须由后端做。如果只返回有数据的月份，前端 x 轴会跳月（比如从 2026-01 直接跳到 2026-08），图表会失真。Prompt 里明确"返回的 months 数组长度固定 12"能避免这个问题。

### P12 Excel 导出

```
实现 spec data-export 的导出需求，对应 tasks 5.2：
- POST /api/export/records，参数 DTO 继承 RecordQueryDTO 并增加 ids 与 exportAll
- 优先级：ids 非空 → 导出选中；exportAll=true → 全量（忽略筛选，但仍受数据隔离）；否则按筛选条件
- EasyExcel 写 RecordExcelVO，列顺序 日期/类型/分类/金额/备注，金额 @NumberFormat("#,##0.00")
- 文件名用 URLEncoder 编码并同时设置 filename 与 filename*=UTF-8''，避免中文乱码
- 前端 responseType: 'blob'，并在保存前识别 Content-Type 是否为 JSON（失败时拦截器返回的是 JSON）
```

**反思**：导出接口的失败响应是 JSON 而非二进制，如果前端不判断 Content-Type，用户会下载到一个内容为错误 JSON 的 `.xlsx`，打开时提示文件损坏。这个细节必须在 Prompt 里点明。

### P13 批量操作与软删除

```
实现 tasks 5.3、5.5：
- 后端批量删除 DELETE /api/finance/records/batch，请求体为 ids 数组
- 删除前先查全部记录，校验数量一致（避免部分 id 不存在时静默成功）与归属
- 前端 el-table 加 selection 列，顶部批量删除/批量导出按钮，未选中时 disabled
- 验证软删除：删除后数据库 is_deleted=1，列表/统计/导出/看板均不统计
```

**反思**：批量删除必须校验"查到的记录数 == 传入的 ids 数"，否则前端传了已删除的 id 也会返回成功，用户会以为删掉了但实际不存在。这类幂等性问题值得在 Prompt 里固化。

### P14 操作日志

```
实现 spec audit 的「操作日志自动记录」，对应 tasks 5.6：
- 自定义 @OperLog(title, businessType) 注解，businessType：1新增 2修改 3删除 4导出 5登录
- AOP 切面 @Around 包裹，finally 里落库，保证成功与失败都记录
- 记录字段：模块、业务类型、操作人、请求方法、URL、参数（JSON 前 512 字符）、结果/错误、IP、耗时
- 参数序列化时对 password 字段脱敏为 ***
- 日志写入失败不能影响业务（catch 后忽略）
- 登录接口也要记录，此时无 SecurityContext，需从入参反射取 username
```

**反思**：登录接口记日志比较特殊 —— 执行时还没有认证上下文，`SecurityUtils.getUsername()` 返回 null。解决方式是遍历入参找 `getUsername` 方法反射调用。虽然有点取巧，但比重构成两套日志机制简单。另外 `@Around` 里一定要用 `finally` 落库，用 `@AfterReturning` 会漏掉失败操作。

---

## 六、问题排查与修复

### P15 布局异常排查

```
现象：登录页卡片被挤在左侧半屏，右侧大片空白。
请先定位根因再改，不要直接调 LoginView 的样式。
提示：检查全局引入的 css 文件里有没有影响 #app 或 body 的布局规则。
```

**产出**：定位到 `assets/main.css` 是 Vue 脚手架首页示例样式，含 `#app { display: grid; grid-template-columns: 1fr 1fr }` 等规则。

**反思**：这类问题的关键是"先定位再改"。如果直接改登录页样式（比如加 `grid-column: span 2`），登录页修好了但主布局仍然错。根因在全局样式，必须全局清理。

### P16 权限响应码统一

```
现状：URL 级鉴权走 EntryPoint/DeniedHandler 返回真实 401/403，
但方法级 @PreAuthorize 抛出的 AccessDeniedException 被 GlobalExceptionHandler 接住，
返回的是 HTTP 200 + body.code=403，两者不一致。
请改成：403/401/400 都返回真实 HTTP 状态码，前端拦截器同步适配。
注意：登录接口自身的 401 是凭据错误，不要触发"登录过期"的登出跳转。
```

**反思**：这是"两套鉴权机制状态码不一致"的典型问题。Spring Security 的异常发生在过滤器链，到不了 `@RestControllerAdvice`；而 `@PreAuthorize` 的异常发生在方法调用阶段，会被 advice 接住。所以统一状态码必须靠 advice 返回 `ResponseEntity` 实现。前端还要区分"登录时的 401"与"token 过期的 401"，否则密码输错会提示"登录已过期"。

---

## 七、整体反思

### 有效的做法

1. **spec 先行**：把需求写进 openspec 的 spec 再让 AI 实现，验收时有明确对照物，避免"AI 说做完了但其实没做"。
2. **Prompt 里带硬约束**：技术栈版本、禁用项（double）、安全要求（密码不返回、数据隔离）、状态码约定，这些写清楚能省掉大量返工。
3. **一任务一提交**：每个 tasks 编号对应一次 commit，出问题能精准回滚。
4. **让 AI 自证**：要求"改完跑编译/类型检查/接口验证"，比"改完告诉我"可靠。

### 踩过的坑（可复用的经验）

1. **可选字段的 null 与空数组语义**：`menuIds` 传空数组会清空权限，这个设计缺陷差点造成数据丢失。
2. **异常类型要区分**：`DisabledException` 不单独处理就会变成 500「系统繁忙」。
3. **两套鉴权状态码不一致**：过滤器链的异常与 `@PreAuthorize` 的异常走的路径不同，需要各自处理才能统一。
4. **前端筛选态与编辑态字段混用**：抽屉里复用 `filter.type` 导致分类选项互相污染。
5. **导出失败返回 JSON**：前端不判断 Content-Type 就会下载到损坏的 xlsx。
6. **脚手架残留**：`create-vue` 的示例样式与示例组件不清理，会在意料之外的地方造成布局问题。

### 对 AI 协作的判断

AI 在"按明确规范生成结构性代码"上效率极高 —— 实体、Mapper、Controller、标准 CRUD 几乎一次成型。但在以下几类场景需要人工把关：

- **隐式的业务语义**（null vs 空数组、统计口径），AI 倾向于选"能跑通"的方案，不一定是"符合 spec"的方案；
- **跨层一致性**（前后端字段约定、状态码约定），单看一层都合理，合起来可能矛盾；
- **框架细节**（Spring Security 异常类型、jjwt 0.12 API 变更），需要用最新版本文档校正；
- **验证环节**，AI 容易把"编译通过"当成"功能正确"，必须要求真实接口调用。

总体来看，AI 承担了约 80% 的代码量，人工集中在设计决策、验收测试与问题排查上，符合考核对"AI 辅助开发"的预期。
