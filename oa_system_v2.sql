/*
 Navicat Premium Dump SQL - 企业OA考勤管理系统v2

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : localhost:3306
 Source Schema         : oa_system

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 19/07/2026 21:30:00
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for app_application
-- ----------------------------
DROP TABLE IF EXISTS `app_application`;
CREATE TABLE `app_application`  (
  `application_id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `user_id` bigint NOT NULL COMMENT '申请人ID',
  `application_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申请类型：LEAVE/请假，OVERTIME/加班，MAKEUP/补卡',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/待审批，APPROVED/已批准，REJECTED/已驳回，CANCELLED/已取消',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`application_id`) USING BTREE,
  INDEX `idx_app_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_app_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `fk_application_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '申请主表';

-- ----------------------------
-- Table structure for app_approval
-- ----------------------------
DROP TABLE IF EXISTS `app_approval`;
CREATE TABLE `app_approval`  (
  `approval_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批ID',
  `application_id` bigint NOT NULL COMMENT '申请ID',
  `approver_id` bigint NOT NULL COMMENT '审批人ID',
  `result` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批结果：APPROVED/通过，REJECTED/拒绝',
  `comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批意见',
  `approve_time` datetime NULL DEFAULT NULL COMMENT '审批时间',
  `approval_level` int DEFAULT 1 COMMENT '审批级别',
  PRIMARY KEY (`approval_id`) USING BTREE,
  INDEX `idx_approval_app`(`application_id` ASC) USING BTREE,
  INDEX `idx_approval_user`(`approver_id` ASC) USING BTREE,
  INDEX `idx_approve_time`(`approve_time` ASC) USING BTREE,
  CONSTRAINT `fk_approval_app` FOREIGN KEY (`application_id`) REFERENCES `app_application` (`application_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_approval_user` FOREIGN KEY (`approver_id`) REFERENCES `sys_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '审批记录表';

-- ----------------------------
-- Table structure for app_leave
-- ----------------------------
DROP TABLE IF EXISTS `app_leave`;
CREATE TABLE `app_leave`  (
  `leave_id` bigint NOT NULL AUTO_INCREMENT COMMENT '请假申请ID',
  `application_id` bigint NOT NULL COMMENT '申请ID',
  `leave_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请假类型：事假、病假、年假、婚假、产假、其他',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请假原因',
  `attachment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '附件路径',
  PRIMARY KEY (`leave_id`) USING BTREE,
  UNIQUE INDEX `uk_application_id`(`application_id` ASC) USING BTREE,
  CONSTRAINT `fk_leave_app` FOREIGN KEY (`application_id`) REFERENCES `app_application` (`application_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '请假申请表';

-- ----------------------------
-- Table structure for app_makeup
-- ----------------------------
DROP TABLE IF EXISTS `app_makeup`;
CREATE TABLE `app_makeup`  (
  `makeup_id` bigint NOT NULL AUTO_INCREMENT COMMENT '补卡申请ID',
  `application_id` bigint NOT NULL COMMENT '申请ID',
  `record_id` bigint NOT NULL COMMENT '考勤记录ID',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '补卡原因',
  `apply_time` datetime NOT NULL COMMENT '申请时间',
  PRIMARY KEY (`makeup_id`) USING BTREE,
  UNIQUE INDEX `uk_application_id`(`application_id` ASC) USING BTREE,
  INDEX `idx_makeup_record`(`record_id` ASC) USING BTREE,
  CONSTRAINT `fk_makeup_app` FOREIGN KEY (`application_id`) REFERENCES `app_application` (`application_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_makeup_record` FOREIGN KEY (`record_id`) REFERENCES `att_record` (`record_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE =utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '补卡申请表';

-- ----------------------------
-- Table structure for app_overtime
-- ----------------------------
DROP TABLE IF EXISTS `app_overtime`;
CREATE TABLE `app_overtime`  (
  `overtime_id` bigint NOT NULL AUTO_INCREMENT COMMENT '加班申请ID',
  `application_id` bigint NOT NULL COMMENT '申请ID',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '加班原因',
  PRIMARY KEY (`overtime_id`) USING BTREE,
  UNIQUE INDEX `uk_application_id`(`application_id` ASC) USING BTREE,
  CONSTRAINT `fk_overtime_app` FOREIGN KEY (`application_id`) REFERENCES `app_application` (`application_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '加班申请表';

-- ----------------------------
-- Table structure for att_record
-- ----------------------------
DROP TABLE IF EXISTS `att_record`;
CREATE TABLE `att_record`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '考勤记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `rule_id` bigint NOT NULL COMMENT '考勤规则ID',
  `attendance_date` date NOT NULL COMMENT '考勤日期',
  `check_in_time` datetime NULL DEFAULT NULL COMMENT '签到时间',
  `check_out_time` datetime NULL DEFAULT NULL COMMENT '签退时间',
  `check_in_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签到状态：NORMAL/正常，LATE/迟到，ABSENT/缺勤',
  `check_out_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签退状态：NORMAL/正常，EARLY/早退，OVERTIME/加班',
  `check_in_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签到位置',
  `check_out_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签退位置',
  `check_in_wifi` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签到时WiFi',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`) USING BTREE,
  INDEX `idx_record_user_date`(`user_id` ASC, `attendance_date` ASC) USING BTREE,
  INDEX `idx_record_rule`(`rule_id` ASC) USING BTREE,
  INDEX `idx_attendance_date`(`attendance_date` ASC) USING BTREE,
  CONSTRAINT `fk_record_rule` FOREIGN KEY (`rule_id`) REFERENCES `att_rule` (`rule_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_record_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '考勤记录表';

-- ----------------------------
-- Table structure for att_rule
-- ----------------------------
DROP TABLE IF EXISTS `att_rule`;
CREATE TABLE `att_rule`  (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID，为空表示全公司通用',
  `work_start_time` time NOT NULL COMMENT '上班时间',
  `work_end_time` time NOT NULL COMMENT '下班时间',
  `late_threshold` int NULL DEFAULT 10 COMMENT '迟到阈值(分钟)',
  `early_threshold` int NULL DEFAULT 10 COMMENT '早退阈值(分钟)',
  `wifi_ssid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '指定WiFi SSID',
  `latitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '经度',
  `radius` int NULL DEFAULT 100 COMMENT '签到半径(米)',
  `enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rule_id`) USING BTREE,
  INDEX `idx_dept_enabled`(`dept_id` ASC, `enabled` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '考勤规则表';

-- ----------------------------
-- Table structure for doc_document
-- ----------------------------
DROP TABLE IF EXISTS `doc_document`;
CREATE TABLE `doc_document`  (
  `doc_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容',
  `author_id` bigint NULL DEFAULT NULL COMMENT '作者ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`doc_id`) USING BTREE,
  INDEX `idx_doc_title`(`title` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `fk_doc_author` FOREIGN KEY (`author_id`) REFERENCES `sys_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '制度文档表';

-- ----------------------------
-- Table structure for sys_department
-- ----------------------------
DROP TABLE IF EXISTS `sys_department`;
CREATE TABLE `sys_department`  (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父级部门ID，0表示顶级部门',
  `dept_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部门名称',
  `dept_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部门代码',
  `leader_id` bigint NULL DEFAULT NULL COMMENT '部门负责人ID',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '部门描述',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`dept_id`) USING BTREE,
  UNIQUE INDEX `uk_dept_code`(`dept_code` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_leader_id`(`leader_id` ASC) USING BTREE,
  CONSTRAINT `fk_dept_leader` FOREIGN KEY (`leader_id`) REFERENCES `sys_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '部门表';

-- 插入默认部门数据
INSERT INTO `sys_department` VALUES (1, 0, '总公司', 'HQ', NULL, '总部', 1, 0, '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_department` VALUES (2, 1, '研发部', 'RD', NULL, '研发部门', 1, 0, '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_department` VALUES (3, 1, '人事部', 'HR', NULL, '人力资源部门', 1, 0, '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_department` VALUES (4, 2, '后端组', 'BE', NULL, '后端开发组', 1, 0, '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_department` VALUES (5, 2, '前端组', 'FE', NULL, '前端开发组', 1, 0, '2026-07-19 20:54:06', '2026-07-19 20:54:06');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父级菜单ID，0表示顶级菜单',
  `menu_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单名称',
  `menu_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '菜单路径',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图标',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `visible` tinyint NULL DEFAULT 1 COMMENT '是否可见',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`menu_id`) USING BTREE,
  INDEX `idx_parent_sort`(`parent_id` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '菜单表';

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `permission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `permission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限代码',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '权限描述',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所属模块',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`permission_id`) USING BTREE,
  UNIQUE INDEX `uk_permission_code`(`permission_code` ASC) USING BTREE,
  INDEX `idx_module`(`module` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '权限表';

-- 插入默认权限数据
INSERT INTO `sys_permission` VALUES (1, '用户新增', 'user:add', '新增员工信息', 'user', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (2, '用户修改', 'user:update', '修改员工信息', 'user', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (3, '用户删除', 'user:delete', '删除员工信息', 'user', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (4, '用户查询', 'user:query', '查询员工信息', 'user', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (5, '部门新增', 'department:add', '新增部门信息', 'department', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (6, '部门修改', 'department:update', '修改部门信息', 'department', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (7, '部门删除', 'department:delete', '删除部门信息', 'department', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (8, '部门查询', 'department:query', '查询部门信息', 'department', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (9, '职位新增', 'position:add', '新增职位信息', 'position', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (10, '职位修改', 'position:update', '修改职位信息', 'position', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (11, '职位删除', 'position:delete', '删除职位信息', 'position', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (12, '职位查询', 'position:query', '查询职位信息', 'position', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (13, '菜单新增', 'menu:add', '新增菜单信息', 'menu', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (14, '菜单修改', 'menu:update', '修改菜单信息', 'menu', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (15, '菜单删除', 'menu:delete', '删除菜单信息', 'menu', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (16, '菜单查询', 'menu:query', '查询菜单信息', 'menu', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (17, '制度文档新增', 'document:add', '新增制度文档', 'document', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (18, '制度文档修改', 'document:update', '修改制度文档', 'document', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (19, '制度文档删除', 'document:delete', '删除制度文档', 'document', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (20, '制度文档查询', 'document:query', '制度文档全文检索', 'document', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_permission` VALUES (21, '制度文档索引', 'document:index', '重建制度文档ES索引', 'document', '2026-07-19 20:54:06', '2026-07-19 20:54:06');

-- ----------------------------
-- Table structure for sys_position
-- ----------------------------
DROP TABLE IF EXISTS `sys_position`;
CREATE TABLE `sys_position`  (
  `position_id` bigint NOT NULL AUTO_INCREMENT COMMENT '职位ID',
  `position_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位名称',
  `dept_id` bigint NOT NULL COMMENT '所属部门ID',
  `level` int NULL DEFAULT 1 COMMENT '职位等级',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职位描述',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`position_id`) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE,
  CONSTRAINT `fk_position_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_department` (`dept_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '职位表';

-- 插入默认职位数据
INSERT INTO `sys_position` VALUES (1, '普通员工', 2, 1, '普通员工职位', 0, '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_position` VALUES (2, '部门主管', 2, 3, '部门主管职位', 0, '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_position` VALUES (3, '系统管理员', 1, 5, '系统管理员职位', 0, '2026-07-19 20:54:06', '2026-07-19 20:54:06');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色代码',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '角色表';

-- 插入默认角色数据
INSERT INTO `sys_role` VALUES (1, '员工', 'EMPLOYEE', '普通员工角色', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_role` VALUES (2, '主管', 'MANAGER', '部门主管角色', '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `sys_role` VALUES (3, '管理员', 'ADMIN', '系统管理员角色', '2026-07-19 20:54:06', '2026-07-19 20:54:06');

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_menu`(`role_id` ASC, `menu_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE,
  CONSTRAINT `fk_role_menu_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_role_menu_menu` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`menu_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '角色菜单关联表';

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_permission`(`role_id` ASC, `permission_id` ASC) USING BTREE,
  INDEX `idx_permission_id`(`permission_id` ASC) USING BTREE,
  CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`permission_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '角色权限关联表';

-- 插入默认角色权限数据
INSERT INTO `sys_role_permission` VALUES (1, 1, 4, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (2, 1, 8, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (3, 1, 12, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (4, 2, 2, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (5, 2, 4, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (6, 2, 8, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (7, 2, 9, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (8, 2, 10, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (9, 2, 12, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (10, 3, 1, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (11, 3, 2, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (12, 3, 3, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (13, 3, 4, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (14, 3, 5, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (15, 3, 6, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (16, 3, 7, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (17, 3, 8, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (18, 3, 9, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (19, 3, 10, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (20, 3, 11, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (21, 3, 12, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (22, 3, 13, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (23, 3, 14, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (24, 3, 15, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (25, 3, 16, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (26, 1, 20, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (27, 2, 17, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (28, 2, 18, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (29, 2, 20, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (30, 2, 21, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (31, 3, 17, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (32, 3, 18, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (33, 3, 19, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (34, 3, 20, '2026-07-19 20:54:06');
INSERT INTO `sys_role_permission` VALUES (35, 3, 21, '2026-07-19 20:54:06');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `employee_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工号',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录账号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录密码（BCrypt加密）',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '真实姓名',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像URL',
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '性别：男，女',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `birth_date` date NULL DEFAULT NULL COMMENT '生日',
  `hire_date` date NULL DEFAULT NULL COMMENT '入职日期',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '所属部门ID',
  `position_id` bigint NULL DEFAULT NULL COMMENT '职位ID',
  `role_id` bigint NULL DEFAULT NULL COMMENT '角色ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `uk_employee_no`(`employee_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE,
  INDEX `idx_position_id`(`position_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_user_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_department` (`dept_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_position` FOREIGN KEY (`position_id`) REFERENCES `sys_position` (`position_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '用户表';

-- 插入默认管理员用户
INSERT INTO `sys_user` VALUES (1, 'A0001', 'admin', '$2a$10$A95mGHC9GOEiVkvrQKpLNOX2JVrZVwIBfCfsaHH1wghaI0S1l1Y7O', '系统管理员', 'Admin', NULL, '男', '13800000000', 'admin@oa.com', NULL, '2026-07-19', 1, 3, 3, 1, 0, NULL, '2026-07-19 20:54:06', '2026-07-19 20:54:06');

-- 插入默认制度文档数据
INSERT INTO `doc_document` VALUES (1, '考勤管理制度', '员工应在规定工作时间内完成签到和签退。迟到、早退、缺勤、加班按考勤规则自动计算，特殊情况可发起补卡或请假审批。', 1, '2026-07-19 20:54:06', '2026-07-19 20:54:06');
INSERT INTO `doc_document` VALUES (2, '请假审批制度', '员工请假需提交请假类型、开始时间、结束时间、原因和附件。审批通过后，请假时间段不参与缺勤统计。', 1, '2026-07-19 20:54:06', '2026-07-19 20:54:06');

SET FOREIGN_KEY_CHECKS = 1;
