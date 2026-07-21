SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 考勤模块表结构升级：
-- 1. 规则支持生效期、工作日、签到/签退窗口、WiFi和定位双重校验开关
-- 2. WiFi与办公地点拆分为子表，支持一个规则绑定多个WiFi和多个地点
-- 3. 考勤记录保留规则、部门、WiFi、定位、距离、校验结果等快照
-- 4. 打卡尝试日志保留失败原因，方便排查定位/WiFi问题
-- 5. 日汇总表支撑员工日历、月度统计、部门/全局统计看板

DROP TABLE IF EXISTS `att_daily_summary`;
DROP TABLE IF EXISTS `att_check_log`;
DROP TABLE IF EXISTS `att_record`;
DROP TABLE IF EXISTS `att_rule_location`;
DROP TABLE IF EXISTS `att_rule_wifi`;
DROP TABLE IF EXISTS `att_rule`;

CREATE TABLE `att_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID，为空表示全公司通用',
  `effective_start_date` date NULL DEFAULT NULL COMMENT '生效开始日期',
  `effective_end_date` date NULL DEFAULT NULL COMMENT '生效结束日期',
  `work_start_time` time NOT NULL COMMENT '上班时间',
  `work_end_time` time NOT NULL COMMENT '下班时间',
  `check_in_start_time` time NULL DEFAULT NULL COMMENT '允许签到开始时间',
  `check_in_end_time` time NULL DEFAULT NULL COMMENT '允许签到结束时间',
  `check_out_start_time` time NULL DEFAULT NULL COMMENT '允许签退开始时间',
  `check_out_end_time` time NULL DEFAULT NULL COMMENT '允许签退结束时间',
  `late_threshold` int NULL DEFAULT 10 COMMENT '迟到阈值，单位分钟',
  `early_threshold` int NULL DEFAULT 10 COMMENT '早退阈值，单位分钟',
  `overtime_threshold` int NULL DEFAULT 30 COMMENT '加班判定阈值，单位分钟',
  `work_days` json NULL COMMENT '工作日配置，例如[1,2,3,4,5]，1表示周一',
  `require_wifi` tinyint NULL DEFAULT 1 COMMENT '是否要求WiFi校验：1是 0否',
  `require_location` tinyint NULL DEFAULT 1 COMMENT '是否要求定位校验：1是 0否',
  `enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用：1启用 0停用',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rule_id`) USING BTREE,
  INDEX `idx_rule_dept_enabled`(`dept_id` ASC, `enabled` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_rule_effective`(`effective_start_date` ASC, `effective_end_date` ASC) USING BTREE,
  INDEX `idx_rule_created_by`(`created_by` ASC) USING BTREE,
  CONSTRAINT `fk_att_rule_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_department` (`dept_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_att_rule_creator` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`user_id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '考勤规则表';

CREATE TABLE `att_rule_wifi` (
  `rule_wifi_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则WiFi ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `wifi_ssid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'WiFi SSID',
  `wifi_bssid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'WiFi BSSID/MAC，可为空',
  `enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用：1启用 0停用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rule_wifi_id`) USING BTREE,
  UNIQUE INDEX `uk_rule_wifi`(`rule_id` ASC, `wifi_ssid` ASC, `wifi_bssid` ASC) USING BTREE,
  INDEX `idx_rule_wifi_enabled`(`rule_id` ASC, `enabled` ASC) USING BTREE,
  CONSTRAINT `fk_rule_wifi_rule` FOREIGN KEY (`rule_id`) REFERENCES `att_rule` (`rule_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '考勤规则WiFi表';

CREATE TABLE `att_rule_location` (
  `rule_location_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则地点ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `location_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '地点名称',
  `latitude` decimal(10,6) NOT NULL COMMENT '纬度',
  `longitude` decimal(10,6) NOT NULL COMMENT '经度',
  `radius` int NULL DEFAULT 100 COMMENT '有效半径，单位米',
  `enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用：1启用 0停用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rule_location_id`) USING BTREE,
  INDEX `idx_rule_location_enabled`(`rule_id` ASC, `enabled` ASC) USING BTREE,
  CONSTRAINT `fk_rule_location_rule` FOREIGN KEY (`rule_id`) REFERENCES `att_rule` (`rule_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '考勤规则地点表';

CREATE TABLE `att_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '考勤记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID快照',
  `rule_id` bigint NOT NULL COMMENT '考勤规则ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规则名称快照',
  `attendance_date` date NOT NULL COMMENT '考勤日期',
  `check_in_time` datetime NULL DEFAULT NULL COMMENT '签到时间',
  `check_out_time` datetime NULL DEFAULT NULL COMMENT '签退时间',
  `check_in_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签到状态：NORMAL/LATE/ABSENT',
  `check_out_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签退状态：NORMAL/EARLY/OVERTIME',
  `attendance_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '当日综合状态：NORMAL/LATE/EARLY/ABSENT/LEAVE/OVERTIME',
  `check_in_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签到地址描述',
  `check_in_latitude` decimal(10,6) NULL DEFAULT NULL COMMENT '签到纬度',
  `check_in_longitude` decimal(10,6) NULL DEFAULT NULL COMMENT '签到经度',
  `check_in_wifi_ssid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签到WiFi SSID',
  `check_in_wifi_bssid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签到WiFi BSSID/MAC',
  `check_in_distance` int NULL DEFAULT NULL COMMENT '签到距离最近有效地点米数',
  `check_in_valid` tinyint NULL DEFAULT NULL COMMENT '签到校验是否通过：1通过 0失败',
  `check_in_fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签到失败原因',
  `check_out_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签退地址描述',
  `check_out_latitude` decimal(10,6) NULL DEFAULT NULL COMMENT '签退纬度',
  `check_out_longitude` decimal(10,6) NULL DEFAULT NULL COMMENT '签退经度',
  `check_out_wifi_ssid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签退WiFi SSID',
  `check_out_wifi_bssid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签退WiFi BSSID/MAC',
  `check_out_distance` int NULL DEFAULT NULL COMMENT '签退距离最近有效地点米数',
  `check_out_valid` tinyint NULL DEFAULT NULL COMMENT '签退校验是否通过：1通过 0失败',
  `check_out_fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签退失败原因',
  `work_minutes` int NULL DEFAULT 0 COMMENT '实际工作分钟数',
  `overtime_minutes` int NULL DEFAULT 0 COMMENT '加班分钟数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`) USING BTREE,
  UNIQUE INDEX `uk_record_user_date_rule`(`user_id` ASC, `attendance_date` ASC, `rule_id` ASC) USING BTREE,
  INDEX `idx_record_dept_date`(`dept_id` ASC, `attendance_date` ASC) USING BTREE,
  INDEX `idx_record_rule`(`rule_id` ASC) USING BTREE,
  INDEX `idx_record_status`(`attendance_status` ASC) USING BTREE,
  CONSTRAINT `fk_record_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_record_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_department` (`dept_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_record_rule` FOREIGN KEY (`rule_id`) REFERENCES `att_rule` (`rule_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '考勤记录表';

CREATE TABLE `att_check_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '打卡日志ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `rule_id` bigint NULL DEFAULT NULL COMMENT '规则ID',
  `record_id` bigint NULL DEFAULT NULL COMMENT '考勤记录ID',
  `attendance_date` date NOT NULL COMMENT '考勤日期',
  `check_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '打卡类型：CHECK_IN/CHECK_OUT',
  `check_time` datetime NOT NULL COMMENT '打卡时间',
  `wifi_ssid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'WiFi SSID',
  `wifi_bssid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'WiFi BSSID/MAC',
  `latitude` decimal(10,6) NULL DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10,6) NULL DEFAULT NULL COMMENT '经度',
  `location_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地址描述',
  `distance` int NULL DEFAULT NULL COMMENT '距离最近有效地点米数',
  `wifi_valid` tinyint NULL DEFAULT NULL COMMENT 'WiFi校验是否通过',
  `location_valid` tinyint NULL DEFAULT NULL COMMENT '定位校验是否通过',
  `success` tinyint NULL DEFAULT 0 COMMENT '本次打卡是否成功',
  `fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失败原因',
  `client_info` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '客户端信息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  INDEX `idx_check_log_user_date`(`user_id` ASC, `attendance_date` ASC) USING BTREE,
  INDEX `idx_check_log_record`(`record_id` ASC) USING BTREE,
  INDEX `idx_check_log_success`(`success` ASC) USING BTREE,
  CONSTRAINT `fk_check_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_check_log_rule` FOREIGN KEY (`rule_id`) REFERENCES `att_rule` (`rule_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_check_log_record` FOREIGN KEY (`record_id`) REFERENCES `att_record` (`record_id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '打卡尝试日志表';

CREATE TABLE `att_daily_summary` (
  `summary_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日汇总ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID快照',
  `attendance_date` date NOT NULL COMMENT '考勤日期',
  `rule_id` bigint NULL DEFAULT NULL COMMENT '规则ID',
  `record_id` bigint NULL DEFAULT NULL COMMENT '考勤记录ID',
  `attendance_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '当日状态：NORMAL/LATE/EARLY/ABSENT/LEAVE/OVERTIME',
  `work_minutes` int NULL DEFAULT 0 COMMENT '工作分钟数',
  `late_minutes` int NULL DEFAULT 0 COMMENT '迟到分钟数',
  `early_minutes` int NULL DEFAULT 0 COMMENT '早退分钟数',
  `overtime_minutes` int NULL DEFAULT 0 COMMENT '加班分钟数',
  `leave_minutes` int NULL DEFAULT 0 COMMENT '请假分钟数',
  `is_late` tinyint NULL DEFAULT 0 COMMENT '是否迟到',
  `is_absent` tinyint NULL DEFAULT 0 COMMENT '是否缺勤',
  `is_overtime` tinyint NULL DEFAULT 0 COMMENT '是否加班',
  `is_leave` tinyint NULL DEFAULT 0 COMMENT '是否请假',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`summary_id`) USING BTREE,
  UNIQUE INDEX `uk_summary_user_date_rule`(`user_id` ASC, `attendance_date` ASC, `rule_id` ASC) USING BTREE,
  INDEX `idx_summary_dept_date`(`dept_id` ASC, `attendance_date` ASC) USING BTREE,
  INDEX `idx_summary_status`(`attendance_status` ASC) USING BTREE,
  INDEX `idx_summary_rule`(`rule_id` ASC) USING BTREE,
  CONSTRAINT `fk_summary_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_summary_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_department` (`dept_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_summary_rule` FOREIGN KEY (`rule_id`) REFERENCES `att_rule` (`rule_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_summary_record` FOREIGN KEY (`record_id`) REFERENCES `att_record` (`record_id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '考勤日汇总表';

-- 补充考勤相关字典
INSERT IGNORE INTO `sys_dict_type` (`dict_type_id`, `dict_name`, `dict_code`, `status`, `remark`) VALUES
(6, '打卡类型', 'attendance_check_type', 1, '签到或签退'),
(7, '打卡校验项', 'attendance_check_item', 1, 'WiFi、定位等校验项'),
(8, '考勤规则状态', 'attendance_rule_status', 1, '考勤规则启用状态');

INSERT IGNORE INTO `sys_dict_data` (`dict_code`, `dict_label`, `dict_value`, `sort_order`, `status`, `remark`) VALUES
('attendance_status', '请假', 'LEAVE', 6, 1, '审批通过的请假'),
('attendance_check_type', '签到', 'CHECK_IN', 1, 1, '上班签到'),
('attendance_check_type', '签退', 'CHECK_OUT', 2, 1, '下班签退'),
('attendance_check_item', 'WiFi', 'WIFI', 1, 1, 'WiFi校验'),
('attendance_check_item', '定位', 'LOCATION', 2, 1, '定位校验'),
('attendance_rule_status', '启用', '1', 1, 1, '规则启用'),
('attendance_rule_status', '停用', '0', 2, 1, '规则停用');

SET FOREIGN_KEY_CHECKS = 1;
