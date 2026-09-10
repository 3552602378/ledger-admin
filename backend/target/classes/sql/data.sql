-- 初始化种子数据
-- 默认密码：admin123 / user123（BCrypt 加密）

USE ledger;

-- 1. 角色
INSERT INTO sys_role (id, role_name, role_code, status, remark) VALUES
(1, '超级管理员', 'admin', 1, '拥有全部权限'),
(2, '普通用户', 'user', 1, '仅可管理个人账务');

-- 2. 菜单（目录、菜单、按钮）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
-- 首页
(1, 0, '首页', 2, '/dashboard', 'DashboardView.vue', 'dashboard:view', 'HomeFilled', 1, 1),

-- 系统管理目录
(10, 0, '系统管理', 1, '/system', NULL, NULL, 'Setting', 10, 1),
(11, 10, '用户管理', 2, '/system/users', 'UserView.vue', 'system:user:view', 'UserFilled', 1, 1),
(12, 11, '用户新增', 3, NULL, NULL, 'system:user:add', NULL, 0, 1),
(13, 11, '用户编辑', 3, NULL, NULL, 'system:user:edit', NULL, 0, 1),
(14, 11, '用户删除', 3, NULL, NULL, 'system:user:delete', NULL, 0, 1),
(15, 11, '重置密码', 3, NULL, NULL, 'system:user:reset', NULL, 0, 1),
(21, 10, '角色管理', 2, '/system/roles', 'RoleView.vue', 'system:role:view', 'User', 2, 1),
(22, 21, '角色新增', 3, NULL, NULL, 'system:role:add', NULL, 0, 1),
(23, 21, '角色编辑', 3, NULL, NULL, 'system:role:edit', NULL, 0, 1),
(24, 21, '角色删除', 3, NULL, NULL, 'system:role:delete', NULL, 0, 1),
(31, 10, '菜单管理', 2, '/system/menus', 'MenuView.vue', 'system:menu:view', 'Menu', 3, 1),
(32, 31, '菜单新增', 3, NULL, NULL, 'system:menu:add', NULL, 0, 1),
(33, 31, '菜单编辑', 3, NULL, NULL, 'system:menu:edit', NULL, 0, 1),
(34, 31, '菜单删除', 3, NULL, NULL, 'system:menu:delete', NULL, 0, 1),

-- 财务管理目录
(50, 0, '财务管理', 1, '/finance', NULL, NULL, 'Money', 20, 1),
(51, 50, '收支分类', 2, '/finance/categories', 'CategoryView.vue', 'finance:category:view', 'FolderOpened', 1, 1),
(52, 51, '分类新增', 3, NULL, NULL, 'finance:category:add', NULL, 0, 1),
(53, 51, '分类编辑', 3, NULL, NULL, 'finance:category:edit', NULL, 0, 1),
(54, 51, '分类删除', 3, NULL, NULL, 'finance:category:delete', NULL, 0, 1),
(61, 50, '收支记录', 2, '/finance/records', 'RecordView.vue', 'finance:record:view', 'Document', 2, 1),
(62, 61, '记录新增', 3, NULL, NULL, 'finance:record:add', NULL, 0, 1),
(63, 61, '记录编辑', 3, NULL, NULL, 'finance:record:edit', NULL, 0, 1),
(64, 61, '记录删除', 3, NULL, NULL, 'finance:record:delete', NULL, 0, 1),
(65, 61, '记录导出', 3, NULL, NULL, 'finance:record:export', NULL, 0, 1),

-- 审计日志
(80, 0, '审计日志', 2, '/audit/logs', 'OperLogView.vue', 'audit:log:view', 'DocumentCopy', 30, 1);

-- 3. 用户由后端 InitDataRunner 在启动时初始化（密码使用 Spring BCryptPasswordEncoder 生成）
-- 默认账号：admin/admin123, user/user123

-- 4. 用户角色关联（用户初始化后由 runner 写入）

-- 5. 角色菜单关联
-- admin 拥有全部菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE status = 1;

-- user 仅拥有财务相关菜单/按钮 + 首页
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1),
(2, 50), (2, 51), (2, 52), (2, 53), (2, 54),
(2, 61), (2, 62), (2, 63), (2, 64), (2, 65);

-- 6. 示例收支分类
INSERT INTO fin_category (id, type, name) VALUES
(1, 'income', '工资'),
(2, 'income', '奖金'),
(3, 'income', '投资收益'),
(4, 'expense', '餐饮'),
(5, 'expense', '交通'),
(6, 'expense', '购物'),
(7, 'expense', '房租');
