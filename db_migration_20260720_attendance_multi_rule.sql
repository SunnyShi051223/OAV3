SET NAMES utf8mb4;

SET @sql := (
  SELECT IF(
    NOT EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'att_record'
        AND index_name = 'uk_record_user_date_rule'
    ),
    'ALTER TABLE att_record ADD UNIQUE INDEX uk_record_user_date_rule(user_id, attendance_date, rule_id)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'att_record'
        AND index_name = 'uk_record_user_date'
    ),
    'ALTER TABLE att_record DROP INDEX uk_record_user_date',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    NOT EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'att_daily_summary'
        AND index_name = 'uk_summary_user_date_rule'
    ),
    'ALTER TABLE att_daily_summary ADD UNIQUE INDEX uk_summary_user_date_rule(user_id, attendance_date, rule_id)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'att_daily_summary'
        AND index_name = 'uk_summary_user_date'
    ),
    'ALTER TABLE att_daily_summary DROP INDEX uk_summary_user_date',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
