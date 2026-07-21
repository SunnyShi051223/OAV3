USE `oa_system`;

-- Business data keeps a department snapshot so later department changes do not alter approval scope.
SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = @schema_name AND table_name = 'app_application' AND column_name = 'applicant_dept_id'),
  'SELECT 1',
  'ALTER TABLE app_application ADD COLUMN applicant_dept_id BIGINT NULL COMMENT ''申请时部门ID'' AFTER applicant_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = @schema_name AND table_name = 'app_application' AND column_name = 'applicant_dept_name'),
  'SELECT 1',
  'ALTER TABLE app_application ADD COLUMN applicant_dept_name VARCHAR(100) NULL COMMENT ''申请时部门名称'' AFTER applicant_dept_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = @schema_name AND table_name = 'app_application' AND column_name = 'complete_time'),
  'SELECT 1',
  'ALTER TABLE app_application ADD COLUMN complete_time DATETIME NULL COMMENT ''流程完成时间'' AFTER process_instance_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics
         WHERE table_schema = @schema_name AND table_name = 'app_application' AND index_name = 'idx_app_dept_status'),
  'SELECT 1',
  'ALTER TABLE app_application ADD INDEX idx_app_dept_status (applicant_dept_id, status)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Approval menu: all roles see their applications; managers/admins also see task handling.
INSERT INTO sys_menu (parent_id, menu_name, menu_path, component, icon, sort_order, visible, deleted, create_time, update_time)
SELECT 0, '审批管理', '/approvals', 'LayoutView', 'approval', 4, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/approvals' AND deleted = 0);

SET @approval_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_path = '/approvals' AND deleted = 0 ORDER BY menu_id LIMIT 1);

INSERT INTO sys_menu (parent_id, menu_name, menu_path, component, icon, sort_order, visible, deleted, create_time, update_time)
SELECT @approval_menu_id, '我的申请', '/approvals/my', 'MyApplicationsView', 'application', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/approvals/my' AND deleted = 0);

INSERT INTO sys_menu (parent_id, menu_name, menu_path, component, icon, sort_order, visible, deleted, create_time, update_time)
SELECT @approval_menu_id, '审批处理', '/approvals/tasks', 'ApprovalTasksView', 'task', 2, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/approvals/tasks' AND deleted = 0);

INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '发起审批', 'approval:submit', '发起统一申请审批', 'approval', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'approval:submit');

INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '查看本人申请', 'approval:self', '查看本人申请及流程状态', 'approval', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'approval:self');

INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '处理审批', 'approval:handle', '处理本部门或全局审批任务', 'approval', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'approval:handle');

SET @approval_my_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_path = '/approvals/my' AND deleted = 0 ORDER BY menu_id LIMIT 1);
SET @approval_task_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_path = '/approvals/tasks' AND deleted = 0 ORDER BY menu_id LIMIT 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT role_id, @approval_menu_id, NOW() FROM sys_role WHERE role_code IN ('EMPLOYEE', 'MANAGER', 'ADMIN');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT role_id, @approval_my_menu_id, NOW() FROM sys_role WHERE role_code IN ('EMPLOYEE', 'MANAGER', 'ADMIN');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT role_id, @approval_task_menu_id, NOW() FROM sys_role WHERE role_code IN ('MANAGER', 'ADMIN');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.role_id, p.permission_id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('approval:submit', 'approval:self')
WHERE r.role_code IN ('EMPLOYEE', 'MANAGER', 'ADMIN');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.role_id, p.permission_id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'approval:handle'
WHERE r.role_code IN ('MANAGER', 'ADMIN');

-- All-approvals view (ADMIN only).
INSERT INTO sys_menu (parent_id, menu_name, menu_path, component, icon, sort_order, visible, deleted, create_time, update_time)
SELECT @approval_menu_id, '全部审批', '/approvals/all', 'AllApplicationsView', 'list', 3, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/approvals/all' AND deleted = 0);

SET @approval_all_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_path = '/approvals/all' AND deleted = 0 ORDER BY menu_id LIMIT 1);

INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '查看全部审批', 'approval:all', '查看所有申请记录', 'approval', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'approval:all');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT role_id, @approval_all_menu_id, NOW() FROM sys_role WHERE role_code IN ('ADMIN');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.role_id, p.permission_id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'approval:all'
WHERE r.role_code IN ('ADMIN');
