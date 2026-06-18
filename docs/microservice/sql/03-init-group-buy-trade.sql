SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `group_buy_trade`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'trade_user'@'%' IDENTIFIED BY 'trade_password_123456';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
ON `group_buy_trade`.* TO 'trade_user'@'%';

FLUSH PRIVILEGES;

USE `group_buy_trade`;

DROP TABLE IF EXISTS `group_buy_order_list`;
DROP TABLE IF EXISTS `group_buy_order`;

CREATE TABLE `group_buy_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `team_id` varchar(32) NOT NULL COMMENT '拼团队伍ID',
  `activity_id` bigint NOT NULL COMMENT '活动业务ID快照',
  `activity_name` varchar(128) NOT NULL COMMENT '活动名称快照',
  `source` varchar(16) NOT NULL COMMENT '来源',
  `channel` varchar(16) NOT NULL COMMENT '渠道',
  `original_price` decimal(10,2) NOT NULL COMMENT '原价快照',
  `deduction_price` decimal(10,2) NOT NULL COMMENT '优惠金额快照',
  `pay_price` decimal(10,2) NOT NULL COMMENT '应付金额快照',
  `target_count` int NOT NULL COMMENT '目标成团人数',
  `lock_count` int NOT NULL DEFAULT '0' COMMENT '已锁单人数',
  `complete_count` int NOT NULL DEFAULT '0' COMMENT '已支付人数',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '队伍状态：0拼团中、1已成团、2拼团失败、3成团后部分退款',
  `valid_start_time` datetime NOT NULL COMMENT '队伍有效期开始时间',
  `valid_end_time` datetime NOT NULL COMMENT '队伍有效期结束时间',
  `notify_type` varchar(16) DEFAULT NULL COMMENT '通知类型：MQ、HTTP',
  `notify_mq` varchar(128) DEFAULT NULL COMMENT 'MQ通知路由',
  `notify_url` varchar(256) DEFAULT NULL COMMENT 'HTTP通知地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_team_id` (`team_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_status_valid_end_time` (`status`, `valid_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团队伍订单';

CREATE TABLE `group_buy_order_list` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `team_id` varchar(32) NOT NULL COMMENT '拼团队伍ID',
  `order_id` varchar(32) NOT NULL COMMENT '交易订单ID',
  `activity_id` bigint NOT NULL COMMENT '活动业务ID快照',
  `activity_name` varchar(128) NOT NULL COMMENT '活动名称快照',
  `goods_id` varchar(32) NOT NULL COMMENT '商品ID快照',
  `goods_name` varchar(128) NOT NULL COMMENT '商品名称快照',
  `source` varchar(16) NOT NULL COMMENT '来源',
  `channel` varchar(16) NOT NULL COMMENT '渠道',
  `original_price` decimal(10,2) NOT NULL COMMENT '原价快照',
  `deduction_price` decimal(10,2) NOT NULL COMMENT '优惠金额快照',
  `pay_price` decimal(10,2) NOT NULL COMMENT '应付金额快照',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0待支付、1已支付、2已关闭',
  `out_trade_no` varchar(64) NOT NULL COMMENT '外部交易单号',
  `biz_id` varchar(128) NOT NULL COMMENT '业务动作幂等号',
  `pay_no` varchar(64) DEFAULT NULL COMMENT '支付流水号',
  `pay_amount` decimal(10,2) DEFAULT NULL COMMENT '实际支付金额',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_id` (`order_id`),
  UNIQUE KEY `uq_out_trade_no` (`out_trade_no`),
  UNIQUE KEY `uq_biz_id` (`biz_id`),
  KEY `idx_user_activity_status` (`user_id`, `activity_id`, `status`),
  KEY `idx_team_id` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户交易订单';
