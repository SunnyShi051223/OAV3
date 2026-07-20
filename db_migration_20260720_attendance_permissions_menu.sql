SET NAMES utf8mb4;

INSERT INTO sys_menu (parent_id, menu_name, menu_path, component, icon, sort_order, visible, deleted, create_time, update_time)
SELECT 0, '考勤管理', '/attendance', 'LayoutView', 'calendar', 4, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/attendance' AND deleted = 0);

SET @attendance_menu_id := (SELECT menu_id FROM sys_menu WHERE menu_path = '/attendance' AND deleted = 0 LIMIT 1);

INSERT INTO sys_menu (parent_id, menu_name, menu_path, component, icon, sort_order, visible, deleted, create_time, update_time)
SELECT @attendance_menu_id, '考勤规则', '/attendance/rules', 'AttendanceRuleView', 'rule', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/attendance/rules' AND deleted = 0);

INSERT INTO sys_menu (parent_id, menu_name, menu_path, component, icon, sort_order, visible, deleted, create_time, update_time)
SELECT @attendance_menu_id, '我的考勤', '/attendance/my', 'MyAttendanceView', 'clock', 2, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/attendance/my' AND deleted = 0);

INSERT INTO sys_menu (parent_id, menu_name, menu_path, component, icon, sort_order, visible, deleted, create_time, update_time)
SELECT @attendance_menu_id, '考勤统计', '/attendance/stats', 'AttendanceStatsView', 'chart', 3, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/attendance/stats' AND deleted = 0);

UPDATE sys_menu SET menu_name = '考勤管理', component = 'LayoutView', icon = 'calendar', sort_order = 4, visible = 1, deleted = 0, update_time = NOW()
WHERE menu_path = '/attendance';

UPDATE sys_menu SET parent_id = @attendance_menu_id, menu_name = '考勤规则', component = 'AttendanceRuleView', icon = 'rule', sort_order = 1, visible = 1, deleted = 0, update_time = NOW()
WHERE menu_path = '/attendance/rules';

UPDATE sys_menu SET parent_id = @attendance_menu_id, menu_name = '我的考勤', component = 'MyAttendanceView', icon = 'clock', sort_order = 2, visible = 1, deleted = 0, update_time = NOW()
WHERE menu_path = '/attendance/my';

UPDATE sys_menu SET parent_id = @attendance_menu_id, menu_name = '考勤统计', component = 'AttendanceStatsView', icon = 'chart', sort_order = 3, visible = 1, deleted = 0, update_time = NOW()
WHERE menu_path = '/attendance/stats';

INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '考勤规则新增', 'attendance:rule:add', '新增考勤规则', 'attendance', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'attendance:rule:add');

INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '考勤规则修改', 'attendance:rule:update', '修改考勤规则', 'attendance', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'attendance:rule:update');

INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '考勤规则删除', 'attendance:rule:delete', '删除考勤规则', 'attendance', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'attendance:rule:delete');

INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '考勤规则查询', 'attendance:rule:query', '查询考勤规则', 'attendance', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'attendance:rule:query');

INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '员工打卡', 'attendance:check', '员工签到签退', 'attendance', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'attendance:check');

INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '个人考勤', 'attendance:self', '查看个人考勤', 'attendance', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'attendance:self');

INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '考勤统计', 'attendance:stats', '查看全局或部门考勤统计', 'attendance', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'attendance:stats');

UPDATE sys_permission SET permission_name = '考勤规则新增', description = '新增考勤规则', module = 'attendance', update_time = NOW()
WHERE permission_code = 'attendance:rule:add';

UPDATE sys_permission SET permission_name = '考勤规则修改', description = '修改考勤规则', module = 'attendance', update_time = NOW()
WHERE permission_code = 'attendance:rule:update';

UPDATE sys_permission SET permission_name = '考勤规则删除', description = '删除考勤规则', module = 'attendance', update_time = NOW()
WHERE permission_code = 'attendance:rule:delete';

UPDATE sys_permission SET permission_name = '考勤规则查询', description = '查询考勤规则', module = 'attendance', update_time = NOW()
WHERE permission_code = 'attendance:rule:query';

UPDATE sys_permission SET permission_name = '员工打卡', description = '员工签到签退', module = 'attendance', update_time = NOW()
WHERE permission_code = 'attendance:check';

UPDATE sys_permission SET permission_name = '个人考勤', description = '查看个人考勤', module = 'attendance', update_time = NOW()
WHERE permission_code = 'attendance:self';

UPDATE sys_permission SET permission_name = '考勤统计', description = '查看全局或部门考勤统计', module = 'attendance', update_time = NOW()
WHERE permission_code = 'attendance:stats';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT r.role_id, m.menu_id, NOW()
FROM sys_role r
JOIN sys_menu m ON m.menu_path IN ('/attendance', '/attendance/rules', '/attendance/stats') AND m.deleted = 0
WHERE r.role_code IN ('ADMIN', 'MANAGER');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT r.role_id, m.menu_id, NOW()
FROM sys_role r
JOIN sys_menu m ON m.menu_path IN ('/attendance', '/attendance/my') AND m.deleted = 0
WHERE r.role_code = 'EMPLOYEE';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.role_id, p.permission_id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'attendance:rule:add',
    'attendance:rule:update',
    'attendance:rule:delete',
    'attendance:rule:query',
    'attendance:stats'
)
WHERE r.role_code IN ('ADMIN', 'MANAGER');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.role_id, p.permission_id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('attendance:check', 'attendance:self')
WHERE r.role_code = 'EMPLOYEE';
