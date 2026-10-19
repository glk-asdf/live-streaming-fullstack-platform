-- ============================================================
-- 直播全栈平台 · 数据库初始化脚本（P1）
-- 适用版本：MySQL 8.0+
-- 字符集：utf8mb4 / utf8mb4_unicode_ci
-- 说明：
--   1. 脚本可重复执行（CREATE ... IF NOT EXISTS / INSERT IGNORE）。
--   2. 需要重建表结构时，先手动 DROP TABLE，再执行本脚本。
--   3. 礼物、钱包、回放、举报等表在对应阶段追加，不在本阶段建。
-- ============================================================

-- 显式声明连接字符集，必须放在所有语句之前，避免客户端按 latin1 解读中文。
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `live`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `live`;

-- ------------------------------------------------------------
-- 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_user`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`   VARCHAR(32)  NOT NULL COMMENT '登录名',
    `password`   VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    `nickname`   VARCHAR(32)  NOT NULL COMMENT '昵称',
    `avatar`     VARCHAR(255)          DEFAULT NULL COMMENT '头像地址',
    `bio`        VARCHAR(255)          DEFAULT NULL COMMENT '个人简介',
    `role`       VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT 'USER / ANCHOR / ADMIN',
    `status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 / 1 禁言 / 2 封禁',
    `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- ------------------------------------------------------------
-- 直播分类表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_category`
(
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(32) NOT NULL COMMENT '分类名',
    `icon`       VARCHAR(64)          DEFAULT NULL COMMENT '图标标识',
    `sort`       INT         NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '直播分类表';

-- ------------------------------------------------------------
-- 直播间表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_live_room`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `anchor_id`       BIGINT       NOT NULL COMMENT '主播用户 ID',
    `category_id`     BIGINT                DEFAULT NULL COMMENT '分类 ID',
    `title`           VARCHAR(80)  NOT NULL COMMENT '直播间标题',
    `cover`           VARCHAR(255)          DEFAULT NULL COMMENT '封面地址',
    `stream_key`      VARCHAR(64)  NOT NULL COMMENT '推流密钥，开播时轮换',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未开播 / 1 直播中 / 2 已结束 / 3 封禁',
    `live_start_time` DATETIME              DEFAULT NULL COMMENT '本场开播时间',
    `live_end_time`   DATETIME              DEFAULT NULL COMMENT '本场结束时间',
    `viewer_count`    INT          NOT NULL DEFAULT 0 COMMENT '实时在线人数',
    `peak_viewer`     INT          NOT NULL DEFAULT 0 COMMENT '本场峰值在线',
    `like_count`      BIGINT       NOT NULL DEFAULT 0 COMMENT '点赞数',
    `hot_score`       INT          NOT NULL DEFAULT 0 COMMENT '热度分',
    `replay_status`   TINYINT      NOT NULL DEFAULT 0 COMMENT '0 无回放 / 1 处理中 / 2 可用',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stream_key` (`stream_key`),
    KEY `idx_anchor` (`anchor_id`),
    KEY `idx_status_hot` (`status`, `hot_score`),
    KEY `idx_category` (`category_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '直播间表';

-- ------------------------------------------------------------
-- 关注表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_follow`
(
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT   NOT NULL COMMENT '关注者用户 ID',
    `anchor_id`  BIGINT   NOT NULL COMMENT '被关注主播 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_anchor` (`user_id`, `anchor_id`),
    KEY `idx_anchor` (`anchor_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '关注表';

-- ------------------------------------------------------------
-- 弹幕消息表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_chat_message`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `room_id`    BIGINT       NOT NULL COMMENT '直播间 ID',
    `user_id`    BIGINT       NOT NULL COMMENT '发送者用户 ID',
    `content`    VARCHAR(200) NOT NULL COMMENT '消息内容',
    `type`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0 文本 / 1 礼物 / 2 系统',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`, `id`),
    KEY `idx_user_time` (`user_id`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '弹幕消息表';

-- ------------------------------------------------------------
-- 初始分类
-- ------------------------------------------------------------
INSERT IGNORE INTO `t_category` (`id`, `name`, `icon`, `sort`)
VALUES (1, '游戏', 'game', 10),
       (2, '音乐', 'music', 20),
       (3, '聊天', 'chat', 30),
       (4, '户外', 'outdoor', 40),
       (5, '知识', 'knowledge', 50);

-- ------------------------------------------------------------
-- 测试账号：tester / 123456
-- BCrypt（$2b$10$），Spring Security 的 BCryptPasswordEncoder 可直接校验。
-- ------------------------------------------------------------
INSERT INTO `t_user` (`username`, `password`, `nickname`, `role`, `status`, `created_at`, `updated_at`)
SELECT 'tester',
       '$2b$10$8YLZJsdPYwLQA.QjYG91g.cmYqZlNgJAzX9O.j/OKzcaaSjS3ot0q',
       '测试用户',
       'ANCHOR',
       0,
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_user` WHERE `username` = 'tester');
