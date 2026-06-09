SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `group_buy_activity`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'activity_user'@'%' IDENTIFIED BY 'activity_password_123456';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
ON `group_buy_activity`.* TO 'activity_user'@'%';

FLUSH PRIVILEGES;

USE `group_buy_activity`;

DROP TABLE IF EXISTS `sc_sku_activity`;
DROP TABLE IF EXISTS `sku`;
DROP TABLE IF EXISTS `group_buy_activity`;
DROP TABLE IF EXISTS `group_buy_discount`;

CREATE TABLE `group_buy_discount` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `discount_id` varchar(8) NOT NULL COMMENT '优惠业务ID',
  `discount_name` varchar(64) NOT NULL COMMENT '优惠名称',
  `discount_desc` varchar(256) NOT NULL COMMENT '优惠描述',
  `discount_type` tinyint NOT NULL DEFAULT '0' COMMENT '优惠类型：0基础优惠、1人群专享优惠',
  `market_plan` varchar(4) NOT NULL DEFAULT 'ZJ' COMMENT '营销计划：ZJ直减、MJ满减、ZK折扣、N固定价',
  `market_expr` varchar(32) NOT NULL COMMENT '营销表达式，由营销计划对应策略解析',
  `tag_id` varchar(32) DEFAULT NULL COMMENT '专享优惠人群标签ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_discount_id` (`discount_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团优惠配置';

INSERT INTO `group_buy_discount`
  (`id`, `discount_id`, `discount_name`, `discount_desc`, `discount_type`, `market_plan`, `market_expr`, `tag_id`, `create_time`, `update_time`)
VALUES
  (1, '25120207', '直减优惠20元', '直减优惠20元', 0, 'ZJ', '20', NULL, '2024-12-07 10:20:15', '2024-12-22 12:09:45'),
  (2, '25120208', '满减优惠100-10元', '满减优惠100-10元', 0, 'MJ', '100,10', NULL, '2024-12-07 10:20:15', '2024-12-22 12:09:47'),
  (3, '25120209', '折扣优惠8折', '折扣优惠8折', 0, 'ZK', '0.8', NULL, '2024-12-07 10:20:15', '2024-12-22 12:11:36'),
  (4, '25120210', 'N元购买优惠', 'N元购买优惠', 0, 'N', '1.99', NULL, '2024-12-07 10:20:15', '2024-12-22 12:11:39');

CREATE TABLE `group_buy_activity` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `activity_id` bigint NOT NULL COMMENT '活动业务ID',
  `activity_name` varchar(128) NOT NULL COMMENT '活动名称',
  `discount_id` varchar(8) NOT NULL COMMENT '关联优惠业务ID',
  `group_type` tinyint NOT NULL DEFAULT '0' COMMENT '拼团方式：0自动成团、1达成目标拼团',
  `take_limit_count` int NOT NULL DEFAULT '1' COMMENT '单人参与次数限制',
  `target` int NOT NULL DEFAULT '1' COMMENT '成团目标人数',
  `valid_time` int NOT NULL DEFAULT '15' COMMENT '拼团有效时间，单位分钟',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '活动状态：0创建、1生效、2过期、3废弃',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '活动开始时间',
  `end_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '活动结束时间',
  `tag_id` varchar(32) DEFAULT NULL COMMENT '活动门禁人群标签ID',
  `tag_scope` varchar(4) DEFAULT NULL COMMENT '标签作用范围：1可见限制、2参与限制',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_activity_id` (`activity_id`),
  KEY `idx_discount_id` (`discount_id`),
  KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团活动配置';

INSERT INTO `group_buy_activity`
  (`id`, `activity_id`, `activity_name`, `discount_id`, `group_type`, `take_limit_count`, `target`, `valid_time`, `status`, `start_time`, `end_time`, `tag_id`, `tag_scope`, `create_time`, `update_time`)
VALUES
  (1, 100123, '测试拼团活动', '25120208', 0, 1, 3, 15, 1, '2024-12-07 10:19:40', '2036-12-31 23:59:59', 'RQ_KJHKL98UU78H66554GFDV', '1,2', '2024-12-07 10:19:40', '2026-06-09 00:00:00');

CREATE TABLE `sc_sku_activity` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `source` varchar(8) NOT NULL COMMENT '来源标识',
  `channel` varchar(8) NOT NULL COMMENT '渠道标识',
  `activity_id` bigint NOT NULL COMMENT '关联活动业务ID',
  `goods_id` varchar(16) NOT NULL COMMENT '商品业务ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_source_channel_goods_id` (`source`, `channel`, `goods_id`),
  KEY `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='来源渠道商品活动关系';

INSERT INTO `sc_sku_activity`
  (`id`, `source`, `channel`, `activity_id`, `goods_id`, `create_time`, `update_time`)
VALUES
  (1, 's01', 'c01', 100123, '9890001', '2025-01-01 13:15:54', '2025-01-01 13:15:54');

CREATE TABLE `sku` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `source` varchar(8) NOT NULL COMMENT '来源标识',
  `channel` varchar(8) NOT NULL COMMENT '渠道标识',
  `goods_id` varchar(16) NOT NULL COMMENT '商品业务ID',
  `goods_name` varchar(128) NOT NULL COMMENT '商品名称',
  `original_price` decimal(10,2) NOT NULL COMMENT '商品原价',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_source_channel_goods_id` (`source`, `channel`, `goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品价格快照';

INSERT INTO `sku`
  (`id`, `source`, `channel`, `goods_id`, `goods_name`, `original_price`, `create_time`, `update_time`)
VALUES
  (1, 's01', 'c01', '9890001', '《手写MyBatis：渐进式源码实践》', 100.00, '2024-12-21 11:10:06', '2024-12-21 11:10:06');
