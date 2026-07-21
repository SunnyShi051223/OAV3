SET NAMES utf8mb4;

-- 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL COMMENT '发送人ID',
    receiver_id BIGINT NOT NULL COMMENT '接收人ID',
    content TEXT NOT NULL COMMENT '消息内容',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读 0未读 1已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_conversation (sender_id, receiver_id),
    INDEX idx_receiver_read (receiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息';

-- 聊天权限
INSERT INTO sys_permission (permission_name, permission_code, description, module, create_time, update_time)
SELECT '聊天通讯', 'chat:query', '聊天模块权限', 'chat', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'chat:query');

-- 聊天菜单
INSERT INTO sys_menu (parent_id, menu_name, menu_path, component, icon, sort_order, visible, deleted, create_time, update_time)
SELECT 0, '聊天', '/chat', 'ChatView', 'message', 5, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/chat' AND deleted = 0);

-- 为所有角色分配菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT r.role_id, m.menu_id, NOW()
FROM sys_role r
JOIN sys_menu m ON m.menu_path = '/chat' AND m.deleted = 0
WHERE r.role_code IN ('ADMIN', 'MANAGER', 'EMPLOYEE');

-- 为所有角色分配权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id, create_time)
SELECT r.role_id, p.permission_id, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'chat:query'
WHERE r.role_code IN ('ADMIN', 'MANAGER', 'EMPLOYEE');
