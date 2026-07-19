-- OA Attendance System (Optimized)
DROP DATABASE IF EXISTS oa_system;
CREATE DATABASE oa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE oa_system;

CREATE TABLE sys_role (
  role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_name VARCHAR(50) NOT NULL,
  role_code VARCHAR(50) NOT NULL UNIQUE,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_department (
  dept_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT DEFAULT 0,
  dept_name VARCHAR(100) NOT NULL,
  leader_id BIGINT NULL,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_position (
  position_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  position_name VARCHAR(100) NOT NULL,
  dept_id BIGINT NOT NULL,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_menu (
  menu_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT DEFAULT 0,
  menu_name VARCHAR(100) NOT NULL,
  menu_path VARCHAR(200),
  component VARCHAR(200),
  icon VARCHAR(100),
  sort_order INT DEFAULT 0,
  visible TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_permission (
  permission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  permission_name VARCHAR(100) NOT NULL,
  permission_code VARCHAR(100) UNIQUE NOT NULL,
  description VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_no VARCHAR(50) UNIQUE NOT NULL,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  gender VARCHAR(10),
  phone VARCHAR(20),
  email VARCHAR(100),
  dept_id BIGINT,
  position_id BIGINT,
  role_id BIGINT,
  status TINYINT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_dept FOREIGN KEY (dept_id) REFERENCES sys_department(dept_id),
  CONSTRAINT fk_user_pos FOREIGN KEY (position_id) REFERENCES sys_position(position_id),
  CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role(role_id)
);

CREATE TABLE sys_role_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  UNIQUE(role_id,menu_id),
  FOREIGN KEY(role_id) REFERENCES sys_role(role_id),
  FOREIGN KEY(menu_id) REFERENCES sys_menu(menu_id)
);

CREATE TABLE sys_role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  UNIQUE(role_id,permission_id),
  FOREIGN KEY(role_id) REFERENCES sys_role(role_id),
  FOREIGN KEY(permission_id) REFERENCES sys_permission(permission_id)
);

CREATE TABLE att_rule (
  rule_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dept_id BIGINT NOT NULL,
  rule_name VARCHAR(100) NOT NULL,
  work_start_time TIME NOT NULL,
  work_end_time TIME NOT NULL,
  late_threshold INT DEFAULT 10,
  early_threshold INT DEFAULT 10,
  wifi_ssid VARCHAR(100),
  latitude DECIMAL(10,6),
  longitude DECIMAL(10,6),
  radius INT DEFAULT 100,
  enabled TINYINT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(dept_id) REFERENCES sys_department(dept_id)
);

CREATE TABLE att_record (
  record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  rule_id BIGINT NOT NULL,
  attendance_date DATE NOT NULL,
  check_in_time DATETIME,
  check_out_time DATETIME,
  check_in_status VARCHAR(20) COMMENT 'NORMAL,LATE,ABSENT',
  check_out_status VARCHAR(20) COMMENT 'NORMAL,EARLY,OVERTIME,ABSENT',
  check_in_location VARCHAR(255),
  check_out_location VARCHAR(255),
  check_in_wifi VARCHAR(100),
  check_out_wifi VARCHAR(100),
  check_in_latitude DECIMAL(10,6),
  check_in_longitude DECIMAL(10,6),
  check_out_latitude DECIMAL(10,6),
  check_out_longitude DECIMAL(10,6),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(user_id) REFERENCES sys_user(user_id),
  FOREIGN KEY(rule_id) REFERENCES att_rule(rule_id)
);

CREATE TABLE app_application (
  application_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  application_type VARCHAR(20) NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING',
  cancel_reason VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(user_id) REFERENCES sys_user(user_id)
);

CREATE TABLE app_leave (
  leave_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_id BIGINT UNIQUE NOT NULL,
  leave_type VARCHAR(50),
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  reason VARCHAR(500),
  FOREIGN KEY(application_id) REFERENCES app_application(application_id)
);

CREATE TABLE app_overtime (
  overtime_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_id BIGINT UNIQUE NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  reason VARCHAR(500),
  FOREIGN KEY(application_id) REFERENCES app_application(application_id)
);

CREATE TABLE app_makeup (
  makeup_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_id BIGINT UNIQUE NOT NULL,
  record_id BIGINT NOT NULL,
  reason VARCHAR(500),
  FOREIGN KEY(application_id) REFERENCES app_application(application_id),
  FOREIGN KEY(record_id) REFERENCES att_record(record_id)
);

CREATE TABLE app_approval (
  approval_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_id BIGINT NOT NULL,
  approver_id BIGINT NOT NULL,
  approval_level INT DEFAULT 1,
  approval_order INT DEFAULT 1,
  result VARCHAR(20),
  comment VARCHAR(500),
  approve_time DATETIME,
  FOREIGN KEY(application_id) REFERENCES app_application(application_id),
  FOREIGN KEY(approver_id) REFERENCES sys_user(user_id)
);

CREATE TABLE doc_document (
  doc_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  content LONGTEXT,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_att_record_user_date ON att_record(user_id,attendance_date);
CREATE INDEX idx_app_user ON app_application(user_id);
CREATE INDEX idx_app_status ON app_application(status);

INSERT INTO sys_role(role_name,role_code) VALUES
('员工','EMPLOYEE'),('主管','MANAGER'),('管理员','ADMIN');
