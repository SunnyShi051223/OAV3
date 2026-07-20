SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 数据字典：统一维护系统枚举、下拉项和可扩展业务类型
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
  `dict_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典类型ID',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典名称',
  `dict_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典编码',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`dict_type_id`) USING BTREE,
  UNIQUE INDEX `uk_dict_code`(`dict_code` ASC) USING BTREE,
  INDEX `idx_dict_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '字典类型表';

CREATE TABLE IF NOT EXISTS `sys_dict_data` (
  `dict_data_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
  `dict_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典编码',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典值',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`dict_data_id`) USING BTREE,
  UNIQUE INDEX `uk_dict_code_value`(`dict_code` ASC, `dict_value` ASC) USING BTREE,
  INDEX `idx_dict_data_code`(`dict_code` ASC, `status` ASC, `sort_order` ASC) USING BTREE,
  CONSTRAINT `fk_dict_data_type` FOREIGN KEY (`dict_code`) REFERENCES `sys_dict_type` (`dict_code`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '字典数据表';

-- 2. 审批业务表：删除旧的审批记录表和请假/补卡/加班子表，统一为一张申请表；流程流转交给 Flowable
DROP TABLE IF EXISTS `app_approval`;
DROP TABLE IF EXISTS `app_leave`;
DROP TABLE IF EXISTS `app_makeup`;
DROP TABLE IF EXISTS `app_overtime`;
DROP TABLE IF EXISTS `app_application`;

CREATE TABLE `app_application` (
  `application_id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID',
  `applicant_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申请人姓名',
  `application_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申请类型：LEAVE/请假，OVERTIME/加班，MAKEUP/补卡，可由字典扩展',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请原因',
  `attachment_urls` json NULL COMMENT '申请附件URL列表',
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请备注',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/草稿，PROCESSING/审批中，APPROVED/已通过，REJECTED/已拒绝，CANCELED/已取消',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`application_id`) USING BTREE,
  INDEX `idx_app_applicant`(`applicant_id` ASC) USING BTREE,
  INDEX `idx_app_type_status`(`application_type` ASC, `status` ASC) USING BTREE,
  INDEX `idx_app_time`(`start_time` ASC, `end_time` ASC) USING BTREE,
  INDEX `idx_app_process`(`process_instance_id` ASC) USING BTREE,
  INDEX `idx_app_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `fk_application_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `sys_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '统一申请表';

-- 3. 制度文档标准标签模型：标签独立建表，文档和标签多对多
CREATE TABLE IF NOT EXISTS `doc_tag` (
  `tag_id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `tag_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签编码',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tag_id`) USING BTREE,
  UNIQUE INDEX `uk_doc_tag_code`(`tag_code` ASC) USING BTREE,
  UNIQUE INDEX `uk_doc_tag_name`(`tag_name` ASC) USING BTREE,
  INDEX `idx_doc_tag_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '制度文档标签表';

CREATE TABLE IF NOT EXISTS `doc_document_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `doc_id` bigint NOT NULL COMMENT '文档ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_doc_tag_relation`(`doc_id` ASC, `tag_id` ASC) USING BTREE,
  INDEX `idx_document_tag_doc`(`doc_id` ASC) USING BTREE,
  INDEX `idx_document_tag_tag`(`tag_id` ASC) USING BTREE,
  CONSTRAINT `fk_document_tag_doc` FOREIGN KEY (`doc_id`) REFERENCES `doc_document` (`doc_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_document_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `doc_tag` (`tag_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '制度文档标签关联表';

-- 4. 默认字典数据
INSERT IGNORE INTO `sys_dict_type` (`dict_type_id`, `dict_name`, `dict_code`, `status`, `remark`) VALUES
(1, '申请类型', 'application_type', 1, '统一申请表的业务类型'),
(2, '申请状态', 'application_status', 1, '统一申请表的业务状态'),
(3, '考勤状态', 'attendance_status', 1, '签到、签退和考勤统计状态'),
(4, '用户状态', 'user_status', 1, '用户启用和停用状态'),
(5, '文档标签', 'document_tag', 1, '制度文档检索标签');

INSERT IGNORE INTO `sys_dict_data` (`dict_code`, `dict_label`, `dict_value`, `sort_order`, `status`, `remark`) VALUES
('application_type', '请假', 'LEAVE', 1, 1, '员工请假申请'),
('application_type', '加班', 'OVERTIME', 2, 1, '员工加班申请'),
('application_type', '补卡', 'MAKEUP', 3, 1, '员工补卡申请'),
('application_status', '草稿', 'DRAFT', 1, 1, '尚未提交'),
('application_status', '审批中', 'PROCESSING', 2, 1, 'Flowable流程处理中'),
('application_status', '已通过', 'APPROVED', 3, 1, '流程审批通过'),
('application_status', '已拒绝', 'REJECTED', 4, 1, '流程审批拒绝'),
('application_status', '已取消', 'CANCELED', 5, 1, '申请人取消'),
('attendance_status', '正常', 'NORMAL', 1, 1, '正常考勤'),
('attendance_status', '迟到', 'LATE', 2, 1, '签到迟到'),
('attendance_status', '早退', 'EARLY', 3, 1, '签退早退'),
('attendance_status', '缺勤', 'ABSENT', 4, 1, '缺勤'),
('attendance_status', '加班', 'OVERTIME', 5, 1, '加班'),
('user_status', '启用', '1', 1, 1, '用户可登录'),
('user_status', '停用', '0', 2, 1, '用户不可登录'),
('document_tag', '考勤', 'ATTENDANCE', 1, 1, '考勤制度相关'),
('document_tag', '请假', 'LEAVE', 2, 1, '请假制度相关'),
('document_tag', '补卡', 'MAKEUP', 3, 1, '补卡制度相关'),
('document_tag', '加班', 'OVERTIME', 4, 1, '加班制度相关'),
('document_tag', '审批', 'APPROVAL', 5, 1, '审批流程相关');

-- 5. 默认文档标签数据和示例关联
INSERT IGNORE INTO `doc_tag` (`tag_id`, `tag_name`, `tag_code`, `status`) VALUES
(1, '考勤', 'ATTENDANCE', 1),
(2, '请假', 'LEAVE', 1),
(3, '补卡', 'MAKEUP', 1),
(4, '加班', 'OVERTIME', 1),
(5, '审批', 'APPROVAL', 1);

INSERT IGNORE INTO `doc_document_tag` (`doc_id`, `tag_id`)
SELECT d.doc_id, 1 FROM `doc_document` d WHERE d.title LIKE '%考勤%';

INSERT IGNORE INTO `doc_document_tag` (`doc_id`, `tag_id`)
SELECT d.doc_id, 2 FROM `doc_document` d WHERE d.title LIKE '%请假%' OR d.content LIKE '%请假%';

INSERT IGNORE INTO `doc_document_tag` (`doc_id`, `tag_id`)
SELECT d.doc_id, 3 FROM `doc_document` d WHERE d.title LIKE '%补卡%' OR d.content LIKE '%补卡%';

INSERT IGNORE INTO `doc_document_tag` (`doc_id`, `tag_id`)
SELECT d.doc_id, 4 FROM `doc_document` d WHERE d.title LIKE '%加班%' OR d.content LIKE '%加班%';

INSERT IGNORE INTO `doc_document_tag` (`doc_id`, `tag_id`)
SELECT d.doc_id, 5 FROM `doc_document` d WHERE d.title LIKE '%审批%' OR d.content LIKE '%审批%';

SET FOREIGN_KEY_CHECKS = 1;
