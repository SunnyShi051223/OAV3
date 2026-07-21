USE `oa_system`;

SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = @schema_name AND table_name = 'app_application'
           AND column_name = 'attendance_record_id'),
  'SELECT 1',
  'ALTER TABLE app_application ADD COLUMN attendance_record_id BIGINT NULL COMMENT ''补卡关联考勤记录ID'' AFTER application_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics
         WHERE table_schema = @schema_name AND table_name = 'app_application'
           AND index_name = 'idx_app_attendance_record'),
  'SELECT 1',
  'ALTER TABLE app_application ADD INDEX idx_app_attendance_record (attendance_record_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.referential_constraints
         WHERE BINARY constraint_schema = BINARY @schema_name
           AND constraint_name = 'fk_application_attendance_record'),
  'SELECT 1',
  'ALTER TABLE app_application ADD CONSTRAINT fk_application_attendance_record FOREIGN KEY (attendance_record_id) REFERENCES att_record(record_id) ON DELETE SET NULL ON UPDATE RESTRICT'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
