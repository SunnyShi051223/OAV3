-- Flowable 6.8.0 official MySQL schema. Run once from the project root.
-- Example: mysql -uroot -p oa_system < db_flowable_6_8_0_init.sql
USE `oa_system`;

SOURCE database/flowable/6.8.0/01-common.sql;
SOURCE database/flowable/6.8.0/02-identitylink.sql;
SOURCE database/flowable/6.8.0/03-identitylink-history.sql;
SOURCE database/flowable/6.8.0/04-entitylink.sql;
SOURCE database/flowable/6.8.0/05-entitylink-history.sql;
SOURCE database/flowable/6.8.0/06-eventsubscription.sql;
SOURCE database/flowable/6.8.0/07-task.sql;
SOURCE database/flowable/6.8.0/08-task-history.sql;
SOURCE database/flowable/6.8.0/09-variable.sql;
SOURCE database/flowable/6.8.0/10-variable-history.sql;
SOURCE database/flowable/6.8.0/11-job.sql;
SOURCE database/flowable/6.8.0/12-batch.sql;
SOURCE database/flowable/6.8.0/13-engine.sql;
SOURCE database/flowable/6.8.0/14-history.sql;
