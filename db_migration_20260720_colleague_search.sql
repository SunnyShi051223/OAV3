SET NAMES utf8mb4;

-- 添加同事检索权限
INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '同事查询', 'colleague:query', '同事信息检索权限', 'colleague', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'colleague:query');

-- 添加同事检索菜单
INSERT INTO sys_menu (parent_id, menu_name, menu_path, component, icon, sort_order, visible, deleted, create_time, update_time)
SELECT 0, '同事检索', '/colleagues', 'ColleaguesView', 'team', 3, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/colleagues' AND deleted = 0);

-- 为所有角色分配菜单权限（数据范围由后端 DataScopeService 控制）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT r.role_id, m.menu_id, NOW()
FROM sys_role r
JOIN sys_menu m ON m.menu_path = '/colleagues' AND m.deleted = 0
WHERE r.role_code IN ('ADMIN', 'MANAGER', 'EMPLOYEE');

-- 为所有角色分配接口权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.role_id, p.permission_id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'colleague:query'
WHERE r.role_code IN ('ADMIN', 'MANAGER', 'EMPLOYEE');
