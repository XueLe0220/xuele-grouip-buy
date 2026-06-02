SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `group_buy_tag`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS `group_buy_activity`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS `group_buy_trade`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'tag_user'@'%' IDENTIFIED BY 'tag_password_123456';
CREATE USER IF NOT EXISTS 'activity_user'@'%' IDENTIFIED BY 'activity_password_123456';
CREATE USER IF NOT EXISTS 'trade_user'@'%' IDENTIFIED BY 'trade_password_123456';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
ON `group_buy_tag`.* TO 'tag_user'@'%';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
ON `group_buy_activity`.* TO 'activity_user'@'%';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
ON `group_buy_trade`.* TO 'trade_user'@'%';

FLUSH PRIVILEGES;

USE `group_buy_tag`;

DROP TABLE IF EXISTS `crowd_tags_detail`;
DROP TABLE IF EXISTS `crowd_tags_job`;
DROP TABLE IF EXISTS `crowd_tags`;

CREATE TABLE `crowd_tags` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `tag_id` varchar(32) NOT NULL COMMENT '人群ID',
  `tag_name` varchar(64) NOT NULL COMMENT '人群名称',
  `tag_desc` varchar(256) NOT NULL COMMENT '人群描述',
  `statistics` int NOT NULL COMMENT '人群标签统计量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人群标签';

INSERT INTO `crowd_tags` (`id`, `tag_id`, `tag_name`, `tag_desc`, `statistics`, `create_time`, `update_time`)
VALUES
  (1, 'RQ_KJHKL98UU78H66554GFDV', '潜在消费用户', '潜在消费用户', 11, '2024-12-28 12:53:28', '2025-01-28 08:23:57');

CREATE TABLE `crowd_tags_detail` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `tag_id` varchar(32) NOT NULL COMMENT '人群ID',
  `user_id` varchar(16) NOT NULL COMMENT '用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_tag_user` (`tag_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人群标签明细';

INSERT INTO `crowd_tags_detail` (`id`, `tag_id`, `user_id`, `create_time`, `update_time`)
VALUES
  (1, 'RQ_KJHKL98UU78H66554GFDV', 'xuele', '2024-12-28 14:42:30', '2024-12-28 14:42:30'),
  (2, 'RQ_KJHKL98UU78H66554GFDV', 'xiaofuge', '2024-12-28 14:42:30', '2024-12-28 14:42:30'),
  (3, 'RQ_KJHKL98UU78H66554GFDV', 'liergou', '2024-12-28 14:42:30', '2024-12-28 14:42:30'),
  (4, 'RQ_KJHKL98UU78H66554GFDV', 'xfg01', '2025-01-25 15:44:55', '2025-01-25 15:44:55'),
  (5, 'RQ_KJHKL98UU78H66554GFDV', 'xfg02', '2025-01-25 15:44:55', '2025-01-25 15:44:55'),
  (6, 'RQ_KJHKL98UU78H66554GFDV', 'xfg03', '2025-01-25 15:44:55', '2025-01-25 15:44:55'),
  (7, 'RQ_KJHKL98UU78H66554GFDV', 'xfg04', '2025-01-26 19:10:36', '2025-01-26 19:10:36'),
  (8, 'RQ_KJHKL98UU78H66554GFDV', 'xfg05', '2025-01-26 19:10:36', '2025-01-26 19:10:36'),
  (9, 'RQ_KJHKL98UU78H66554GFDV', 'xfg06', '2025-01-26 19:10:37', '2025-01-26 19:10:37'),
  (10, 'RQ_KJHKL98UU78H66554GFDV', 'xfg07', '2025-01-26 19:10:37', '2025-01-26 19:10:37'),
  (11, 'RQ_KJHKL98UU78H66554GFDV', 'xfg08', '2025-01-26 19:10:37', '2025-01-26 19:10:37');

CREATE TABLE `crowd_tags_job` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `tag_id` varchar(32) NOT NULL COMMENT '标签ID',
  `batch_id` varchar(8) NOT NULL COMMENT '批次ID',
  `tag_type` tinyint NOT NULL DEFAULT '1' COMMENT '标签类型：参与量、消费金额',
  `tag_rule` varchar(8) NOT NULL COMMENT '标签规则：限定类型/次数',
  `stat_start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计数据开始时间',
  `stat_end_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计数据结束时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0初始、1计划、2重置、3完成',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_batch_id` (`batch_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人群标签任务';

INSERT INTO `crowd_tags_job` (`id`, `tag_id`, `batch_id`, `tag_type`, `tag_rule`, `stat_start_time`, `stat_end_time`, `status`, `create_time`, `update_time`)
VALUES
  (1, 'RQ_KJHKL98UU78H66554GFDV', '10001', 0, '100', '2024-12-28 12:55:05', '2024-12-28 12:55:05', 0, '2024-12-28 12:55:05', '2024-12-28 12:55:05');