-- =============================================================================
-- 文件：sql/mall.sql
-- 项目：慕课网商城 mall（Spring Boot + MyBatis）
-- 用途：一键建表，供 clone 项目后初始化数据库使用
--
-- -----------------------------------------------------------------------------
-- 【本 DDL 是怎么来的】
--   原项目仓库中**不存在任何 .sql 文件**，别人 clone 下来无法建库，因此本脚本是
--   「依据代码 + 本机现有库」反推并核对出来的，依据来源及优先级如下：
--     1) src/main/resources/mappers/*.xml 的 <resultMap column/jdbcType>
--        —— 列名与数据类型的**权威来源**（列集合与 jdbcType 以它为准）；
--     2) src/main/java/com/imooc/mall/pojo/*.java —— 确认字段语义；
--     3) src/main/java/com/imooc/mall/enums/*.java —— 确认状态码取值（写 COMMENT 用）；
--     4) src/main/resources/generatorConfig.xml —— 确认表名（该文件中的数据库口令
--        按安全要求**不写入本文件**，本文件不含任何账号/口令/密钥）；
--     5) 本机已存在的 mall 开发库的真实 SHOW CREATE TABLE —— 用于补齐 resultMap
--        无法给出的细节：varchar 长度、默认值、索引名。
--        **列名/类型与 resultMap 完全一致；若两者冲突一律以 resultMap + pojo 为准**
--        （本次核对未出现冲突）。
--
--   凡源码与本机库都无法给出的信息，本文件不做臆造，一律在文末「不确定项」列明。
--
-- 【包含的表】共 6 张，与 resources/mappers 下的 6 个 mapper 一一对应：
--   mall_user / mall_category / mall_product / mall_order / mall_order_item / mall_shipping
--
--   注：支付信息表 mall_pay_info **不包含在本脚本中** —— 该表由支付服务（pay）使用，
--       DDL 的权威版本在 pay 仓库：https://github.com/fanpaisk/pay/blob/main/sql/pay_info.sql
--       两个服务共用同一个库，首次搭建请先执行本脚本、再执行上面那一份。
--       （本项目 dao/ 与 mappers/ 中没有它的 Mapper，代码不直接读写该表。）
--
-- 【字符集】本脚本按需求统一使用 utf8mb4；需注意本机现有库为 utf8(utf8mb3)。
--   utf8mb4 是 utf8 的超集，全新初始化用 utf8mb4 可完整兼容原 utf8 数据。
--
-- 【数据安全 · 务必注意】
--   6 张核心表使用了 DROP TABLE IF EXISTS！在**已有数据的库**上执行会清空这些表
--   （本机 mall 库当前有 user=2 / product=4 / category=32 / order=4 / order_item=8 /
--     shipping=3 条测试数据，DROP 会一并删除）。目标库若已有数据，请先备份，
--     或手工删掉 DROP 语句后再执行。
--
-- 【生成日期】2026-10-06
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. 用户表   mall_user   ← mappers/UserMapper.xml (BaseResultMap)  共 10 列
-- =============================================================================
DROP TABLE IF EXISTS `mall_user`;
CREATE TABLE `mall_user` (
  `id`          int          NOT NULL AUTO_INCREMENT                COMMENT '用户表主键，自增',
  `username`    varchar(50)  NOT NULL                               COMMENT '用户名，唯一（UserMapper.countByUsername 依赖）',
  `password`    varchar(50)  NOT NULL                               COMMENT '密码密文（MD5 为 32 位；若改用 BCrypt 需自行加长此列）',
  `email`       varchar(50)  DEFAULT NULL                           COMMENT '电子邮箱，注册时不可重复（countByEmail 校验）',
  `phone`       varchar(20)  DEFAULT NULL                           COMMENT '手机号',
  `question`    varchar(100) DEFAULT NULL                           COMMENT '找回密码用的密保问题',
  `answer`      varchar(100) DEFAULT NULL                           COMMENT '密保问题的答案',
  `role`        int          NOT NULL DEFAULT 0                     COMMENT '角色：0=管理员(ADMIN)，1=普通用户(CUSTOMER)，见 RoleEnum',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '创建时间',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_name_unique` (`username`) COMMENT '用户名唯一'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='用户表';

-- =============================================================================
-- 2. 商品分类表   mall_category   ← mappers/CategoryMapper.xml (BaseResultMap)  共 7 列
-- =============================================================================
DROP TABLE IF EXISTS `mall_category`;
CREATE TABLE `mall_category` (
  `id`          int         NOT NULL AUTO_INCREMENT                 COMMENT '分类表主键，自增',
  `parent_id`   int         DEFAULT 0                               COMMENT '父分类 id，0 表示一级（顶级）分类',
  `name`        varchar(50) DEFAULT NULL                            COMMENT '分类名称',
  `status`      tinyint(1)  DEFAULT 1                               COMMENT '分类状态：1=正常，0=已废弃（selectAll 只查 status=1）；resultMap jdbcType=BIT',
  `sort_order`  int         NOT NULL DEFAULT 1                      COMMENT '同级排序号，越小越靠前',
  `create_time` datetime    DEFAULT CURRENT_TIMESTAMP               COMMENT '创建时间',
  `update_time` datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_parent_id` (`parent_id`) COMMENT '按父分类查子分类（selectByPrimaryParentKey）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='商品分类表';

-- =============================================================================
-- 3. 商品表   mall_product   ← mappers/ProductMapper.xml (BaseResultMap)  共 12 列
-- =============================================================================
DROP TABLE IF EXISTS `mall_product`;
CREATE TABLE `mall_product` (
  `id`          int            NOT NULL AUTO_INCREMENT              COMMENT '商品表主键，自增',
  `category_id` int            NOT NULL                             COMMENT '所属分类 id，关联 mall_category.id',
  `name`        varchar(100)   NOT NULL                             COMMENT '商品名称',
  `subtitle`    varchar(200)   DEFAULT NULL                         COMMENT '商品副标题/卖点',
  `main_image`  varchar(500)   DEFAULT NULL                         COMMENT '主图相对路径，返回前端时由 imageHost 前缀拼接',
  `sub_images`  text                                                COMMENT '子图路径，多张以英文逗号分隔（前端按 , 切分）；本机库为 text，见文末说明',
  `detail`      text                                                COMMENT '商品详情（富文本 HTML）；本机库为 text，见文末说明',
  `price`       decimal(20, 2) NOT NULL                             COMMENT '售价，单位元（resultMap jdbcType=DECIMAL）',
  `stock`       int            NOT NULL                             COMMENT '库存数量，下单扣减',
  `status`      int            DEFAULT 1                            COMMENT '商品状态：1=在售(ON_SALE)，2=下架(OFF_SALE)，3=已删除(DELETE)，见 ProductStatusEnum',
  `create_time` datetime       DEFAULT CURRENT_TIMESTAMP            COMMENT '创建时间',
  `update_time` datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_category_id` (`category_id`) COMMENT '按分类查商品（selectByCategoryIdSet）',
  KEY `idx_product_status_category` (`status`, `category_id`) COMMENT '前台「在售 + 分类」联合过滤'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='商品表';

-- =============================================================================
-- 4. 订单表   mall_order   ← mappers/OrderMapper.xml (BaseResultMap)  共 14 列
-- =============================================================================
DROP TABLE IF EXISTS `mall_order`;
CREATE TABLE `mall_order` (
  `id`           int            NOT NULL AUTO_INCREMENT             COMMENT '订单表主键，自增',
  `order_no`     bigint         DEFAULT NULL                        COMMENT '订单号，业务唯一（selectByOrderNo 依赖），非主键',
  `user_id`      int            DEFAULT NULL                        COMMENT '下单用户 id，关联 mall_user.id',
  `shipping_id`  int            DEFAULT NULL                        COMMENT '收货地址 id，关联 mall_shipping.id',
  `payment`      decimal(20, 2) DEFAULT NULL                        COMMENT '实付金额，单位元',
  `payment_type` int            DEFAULT NULL                        COMMENT '支付方式：1=支付宝（源码未定义枚举，仅按语义标注）',
  `postage`      int            DEFAULT NULL                        COMMENT '运费，单位元（resultMap jdbcType=INTEGER）',
  `status`       int            DEFAULT NULL                        COMMENT '订单状态：0=已取消，10=未付款，20=已付款，40=已发货，50=交易成功，60=交易关闭，见 OrderStatusEnum',
  `payment_time` datetime       DEFAULT NULL                        COMMENT '付款时间',
  `send_time`    datetime       DEFAULT NULL                        COMMENT '发货时间',
  `end_time`     datetime       DEFAULT NULL                        COMMENT '交易完成时间',
  `close_time`   datetime       DEFAULT NULL                        COMMENT '交易关闭时间',
  `create_time`  datetime       DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `update_time`  datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no_index` (`order_no`) COMMENT '订单号唯一（索引名沿用既有库）',
  KEY `idx_order_user_id` (`user_id`) COMMENT '按用户查订单（selectByUid）',
  KEY `idx_order_status_create_time` (`status`, `create_time`) COMMENT '后台按状态+时间筛选'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='订单表';

-- =============================================================================
-- 5. 订单明细表   mall_order_item   ← mappers/OrderItemMapper.xml (BaseResultMap)  共 11 列
-- =============================================================================
DROP TABLE IF EXISTS `mall_order_item`;
CREATE TABLE `mall_order_item` (
  `id`                 int            NOT NULL AUTO_INCREMENT       COMMENT '订单明细主键，自增',
  `user_id`            int            DEFAULT NULL                  COMMENT '所属用户 id，关联 mall_user.id（冗余，便于按用户查明细）',
  `order_no`           bigint         DEFAULT NULL                  COMMENT '所属订单号，关联 mall_order.order_no',
  `product_id`         int            DEFAULT NULL                  COMMENT '商品 id，关联 mall_product.id',
  `product_name`       varchar(100)   DEFAULT NULL                  COMMENT '下单时的商品名称快照',
  `product_image`      varchar(500)   DEFAULT NULL                  COMMENT '下单时的商品主图快照（相对路径）',
  `current_unit_price` decimal(20, 2) DEFAULT NULL                  COMMENT '下单时的商品单价快照，单位元',
  `quantity`           int            DEFAULT NULL                  COMMENT '购买数量',
  `total_price`        decimal(20, 2) DEFAULT NULL                  COMMENT '本明细小计金额 = current_unit_price * quantity',
  `create_time`        datetime       DEFAULT CURRENT_TIMESTAMP     COMMENT '创建时间',
  `update_time`        datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `order_no_index` (`order_no`) COMMENT '按订单号查明细（selectByOrderNoSet）：普通索引，一单多条',
  KEY `order_no_user_id_index` (`user_id`, `order_no`) COMMENT '按用户+订单号查明细（索引名沿用既有库）',
  KEY `idx_order_item_product_id` (`product_id`) COMMENT '按商品反查（销量/统计类查询）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='订单明细表';

-- =============================================================================
-- 6. 收货地址表   mall_shipping   ← mappers/ShippingMapper.xml (BaseResultMap)  共 12 列
-- =============================================================================
DROP TABLE IF EXISTS `mall_shipping`;
CREATE TABLE `mall_shipping` (
  `id`                int          NOT NULL AUTO_INCREMENT          COMMENT '收货地址主键，自增（insertSelective 用 useGeneratedKeys 回填 id）',
  `user_id`           int          DEFAULT NULL                     COMMENT '所属用户 id，关联 mall_user.id',
  `receiver_name`     varchar(20)  DEFAULT NULL                     COMMENT '收货人姓名',
  `receiver_phone`    varchar(20)  DEFAULT NULL                     COMMENT '收货人固定电话',
  `receiver_mobile`   varchar(20)  DEFAULT NULL                     COMMENT '收货人手机号',
  `receiver_province` varchar(20)  DEFAULT NULL                     COMMENT '省份',
  `receiver_city`     varchar(20)  DEFAULT NULL                     COMMENT '城市',
  `receiver_district` varchar(20)  DEFAULT NULL                     COMMENT '区/县',
  `receiver_address`  varchar(200) DEFAULT NULL                     COMMENT '详细地址',
  `receiver_zip`      varchar(6)   DEFAULT NULL                     COMMENT '邮政编码',
  `create_time`       datetime     DEFAULT CURRENT_TIMESTAMP        COMMENT '创建时间',
  `update_time`       datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_shipping_user_id` (`user_id`) COMMENT '按用户查地址列表（selectByUid）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='收货地址表';

-- =============================================================================
-- 附：支付信息表 mall_pay_info 不在本脚本中
--   DDL 权威版本：https://github.com/fanpaisk/pay/blob/main/sql/pay_info.sql
--   两服务共用同一个库，首次搭建时先执行本脚本、再执行上面那一份。
-- =============================================================================
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 【示例数据】仅插入分类（mall_category），不含任何真实用户或订单数据。
-- 一级分类 parent_id=0，status=1（正常），sort_order 控制前端展示顺序。
-- =============================================================================
INSERT INTO `mall_category` (`id`, `parent_id`, `name`, `status`, `sort_order`, `create_time`, `update_time`) VALUES
  (1, 0, '家用电器', 1, 1, NOW(), NOW()),
  (2, 0, '手机数码', 1, 2, NOW(), NOW()),
  (3, 0, '电脑办公', 1, 3, NOW(), NOW()),
  (4, 0, '服饰鞋包', 1, 4, NOW(), NOW()),
  (5, 0, '食品生鲜', 1, 5, NOW(), NOW());

-- =============================================================================
-- 【自检结论】逐表把本文件的列集合与对应 mapper resultMap 的 column 集合比对：
--   mall_user       10 列 —— 一致（无遗漏、无多余）
--   mall_category    7 列 —— 一致
--   mall_product    12 列 —— 一致
--   mall_order      14 列 —— 一致
--   mall_order_item 11 列 —— 一致
--   mall_shipping   12 列 —— 一致
--
-- 【不确定项 · 明确列出（不猜）】
--   1) mall_product.detail / sub_images 的类型：
--      mapper 的 resultMap 写的是 jdbcType=VARCHAR，但 MySQL 中 VARCHAR 与 TEXT
--      在 JDBC 上同为 LONGVARCHAR，resultMap 无法区分两者；本机库实际为 **text**。
--      本脚本采用 text（不会因详情过长而插入失败）。
--      若必须严格 VARCHAR，可改成 varchar(2000)/varchar(1000)，但超长内容在
--      MySQL 非严格模式下会被**静默截断**。
--   2) 各 VARCHAR 列的长度：resultMap 只给 jdbcType=VARCHAR 不给长度，
--      generatorConfig.xml 亦未配置长度。本脚本长度取自本机库真实 DDL
--      （username/email=50、password=50、question/answer=100、phone=20、
--        商品 name=100、subtitle=200、main_image=500、product_name=100、
--        product_image=500、receiver_*=20、receiver_address=200、receiver_zip=6），
--      属于「可复现的工作库取值」而非源码推导值，换库/换需求请自行调整。
--   3) 索引名：order_no_index / order_no_user_id_index / user_name_unique 沿用既有库
--      命名；其余 idx_* 索引是本脚本新增的**性能建议**，非源码要求
--      （原库并没有 user_id 等索引），若要完全对齐旧库可删去 idx_* 索引。
--   4) 默认值（CURRENT_TIMESTAMP、role=0、status=1 等）均取自本机库；源码只保证
--      「插入时显式赋值」可用，故默认值不影响业务正确性。
-- =============================================================================
