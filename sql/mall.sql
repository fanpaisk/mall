-- =============================================================================
-- 文件：sql/mall.sql
-- 用途：慕课网商城（mall）项目建库建表脚本
--
-- 【重要说明】
--   本 DDL 是「反推」出来的：原项目仓库中**不存在任何 .sql 文件**，
--   别人 clone 下来无法建库，因此依据以下源码反推表结构与列定义：
--     1) src/main/resources/mappers/*.xml 的 <resultMap column/jdbcType> —— 列名与数据类型的权威来源；
--     2) src/main/java/com/imooc/mall/pojo/*.java（实体类，用于确认字段语义与取值范围）；
--     3) src/main/resources/generatorConfig.xml（MyBatis 反向生成配置，用于确认表名）
--        以及 src/main/java/com/imooc/mall/enums/*.java（状态码取值，用于写 COMMENT）。
--   注意：varchar 的具体长度在源码中**没有**任何依据（resultMap 只给出 jdbcType=VARCHAR），
--   下文长度是按字段语义给出的**合理取值**，如需与线上库一比一对齐，请以真实库为准。
--
-- 【生成的表】共 6 张（与 mappers 目录下的 6 个 mapper 一一对应）：
--   mall_user / mall_category / mall_product / mall_order / mall_order_item / mall_shipping
--   说明：generatorConfig.xml 中注释掉、且本项目 mapper/pojo 均未使用的 `mall_pay_info`
--         **未包含**在本脚本中（详见文件末尾备注）。
--   说明：购物车（Cart）不落库，存放在 Redis（见 CartServiceImpl，key 形如 `cart_%d`），故无 cart 表。
--
-- 【安全】本文件不含任何数据库账号、口令或密钥。
-- 【生成日期】2026-10-06
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 用户表   ← mappers/UserMapper.xml (BaseResultMap)  共 10 列
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `mall_user`;
CREATE TABLE `mall_user` (
  `id`          int          NOT NULL AUTO_INCREMENT                COMMENT '用户表主键，自增',
  `username`    varchar(50)  NOT NULL                               COMMENT '用户名，唯一',
  `password`    varchar(100) NOT NULL                               COMMENT '密码（加密后的密文，MD5 占 32 位，预留长度）',
  `email`       varchar(50)  DEFAULT NULL                           COMMENT '电子邮箱，注册时不可重复（countByEmail 校验）',
  `phone`       varchar(20)  DEFAULT NULL                           COMMENT '手机号',
  `question`    varchar(50)  DEFAULT NULL                           COMMENT '找回密码用的密保问题',
  `answer`      varchar(100) DEFAULT NULL                           COMMENT '密保问题的答案',
  `role`        int          NOT NULL DEFAULT 1                     COMMENT '角色：0=管理员(ADMIN)，1=普通用户(CUSTOMER)，见 RoleEnum',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '创建时间',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`) COMMENT '用户名唯一（UserMapper.countByUsername 依赖）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='用户表';

-- -----------------------------------------------------------------------------
-- 2. 商品分类表   ← mappers/CategoryMapper.xml (BaseResultMap)  共 7 列
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `mall_category`;
CREATE TABLE `mall_category` (
  `id`          int         NOT NULL AUTO_INCREMENT                 COMMENT '分类表主键，自增',
  `parent_id`   int         NOT NULL DEFAULT 0                      COMMENT '父分类 id，0 表示一级（顶级）分类',
  `name`        varchar(50) NOT NULL                                COMMENT '分类名称',
  `status`      tinyint(1)  NOT NULL DEFAULT 1                      COMMENT '分类状态：1=正常，0=已废弃（selectAll 只查 status=1）',
  `sort_order`  int         DEFAULT 0                               COMMENT '同级排序号，越小越靠前',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
  `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_parent_id` (`parent_id`) COMMENT '按父分类查询子分类（selectByPrimaryParentKey）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='商品分类表';

-- -----------------------------------------------------------------------------
-- 3. 商品表   ← mappers/ProductMapper.xml (BaseResultMap)  共 12 列
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `mall_product`;
CREATE TABLE `mall_product` (
  `id`          int            NOT NULL AUTO_INCREMENT              COMMENT '商品表主键，自增',
  `category_id` int            NOT NULL                             COMMENT '所属分类 id，关联 mall_category.id',
  `name`        varchar(100)   NOT NULL                             COMMENT '商品名称',
  `subtitle`    varchar(200)   DEFAULT NULL                         COMMENT '商品副标题/卖点',
  `main_image`  varchar(500)   DEFAULT NULL                         COMMENT '主图相对路径，返回前端时由 imageHost 前缀拼接',
  `sub_images`  varchar(1000)  DEFAULT NULL                         COMMENT '子图路径，多张以英文逗号分隔（前端 split(",") 解析）',
  `detail`      varchar(2000)  DEFAULT NULL                         COMMENT '商品详情（富文本或长描述，长度按语义放宽）',
  `price`       decimal(20, 2) NOT NULL DEFAULT 0.00                COMMENT '售价，单位元（resultMap jdbcType=DECIMAL）',
  `stock`       int            NOT NULL DEFAULT 0                   COMMENT '库存数量，下单扣减',
  `status`      int            NOT NULL DEFAULT 1                   COMMENT '商品状态：1=在售(ON_SALE)，2=下架(OFF_SALE)，3=已删除(DELETE)，见 ProductStatusEnum',
  `create_time` datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
  `update_time` datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_category_id` (`category_id`) COMMENT '按分类查商品（selectByCategoryIdSet）',
  KEY `idx_product_status_category` (`status`, `category_id`) COMMENT '前台列表「在售 + 分类」联合过滤'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='商品表';

-- -----------------------------------------------------------------------------
-- 4. 订单表   ← mappers/OrderMapper.xml (BaseResultMap)  共 14 列
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `mall_order`;
CREATE TABLE `mall_order` (
  `id`           int            NOT NULL AUTO_INCREMENT             COMMENT '订单表主键，自增',
  `order_no`     bigint         NOT NULL                            COMMENT '订单号，业务唯一（selectByOrderNo 依赖），非主键',
  `user_id`      int            NOT NULL                            COMMENT '下单用户 id，关联 mall_user.id',
  `shipping_id`  int            DEFAULT NULL                        COMMENT '收货地址 id，关联 mall_shipping.id',
  `payment`      decimal(20, 2) DEFAULT NULL                        COMMENT '实付金额，单位元',
  `payment_type` int            DEFAULT NULL                        COMMENT '支付方式：1=支付宝（源码未定义枚举，仅按语义标注）',
  `postage`      int            NOT NULL DEFAULT 0                  COMMENT '运费，单位元（resultMap jdbcType=INTEGER）',
  `status`       int            NOT NULL DEFAULT 10                 COMMENT '订单状态：0=已取消，10=未付款，20=已付款，40=已发货，50=交易成功，60=交易关闭，见 OrderStatusEnum',
  `payment_time` datetime       DEFAULT NULL                        COMMENT '付款时间',
  `send_time`    datetime       DEFAULT NULL                        COMMENT '发货时间',
  `end_time`     datetime       DEFAULT NULL                        COMMENT '交易完成时间',
  `close_time`   datetime       DEFAULT NULL                        COMMENT '交易关闭时间',
  `create_time`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  `update_time`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_order_no` (`order_no`) COMMENT '订单号唯一',
  KEY `idx_order_user_id` (`user_id`) COMMENT '按用户查订单（selectByUid）',
  KEY `idx_order_status_create_time` (`status`, `create_time`) COMMENT '后台按状态+时间筛选'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='订单表';

-- -----------------------------------------------------------------------------
-- 5. 订单明细表   ← mappers/OrderItemMapper.xml (BaseResultMap)  共 11 列
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `mall_order_item`;
CREATE TABLE `mall_order_item` (
  `id`                 int            NOT NULL AUTO_INCREMENT       COMMENT '订单明细主键，自增',
  `user_id`            int            NOT NULL                      COMMENT '所属用户 id，关联 mall_user.id（冗余，便于按用户查明细）',
  `order_no`           bigint         NOT NULL                      COMMENT '所属订单号，关联 mall_order.order_no',
  `product_id`         int            NOT NULL                      COMMENT '商品 id，关联 mall_product.id',
  `product_name`       varchar(100)   NOT NULL                      COMMENT '下单时的商品名称快照',
  `product_image`      varchar(500)   DEFAULT NULL                  COMMENT '下单时的商品主图快照（相对路径）',
  `current_unit_price` decimal(20, 2) NOT NULL DEFAULT 0.00         COMMENT '下单时的商品单价快照，单位元',
  `quantity`           int            NOT NULL DEFAULT 1            COMMENT '购买数量',
  `total_price`        decimal(20, 2) NOT NULL DEFAULT 0.00         COMMENT '本明细小计金额 = current_unit_price * quantity',
  `create_time`        datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`        datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_item_order_no` (`order_no`) COMMENT '按订单号查明细（selectByOrderNoSet），普通索引：一单多条',
  KEY `idx_order_item_user_id` (`user_id`) COMMENT '按用户查明细',
  KEY `idx_order_item_product_id` (`product_id`) COMMENT '按商品反查（销量/统计类查询）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='订单明细表';

-- -----------------------------------------------------------------------------
-- 6. 收货地址表   ← mappers/ShippingMapper.xml (BaseResultMap)  共 12 列
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `mall_shipping`;
CREATE TABLE `mall_shipping` (
  `id`                int          NOT NULL AUTO_INCREMENT          COMMENT '收货地址主键，自增（insertSelective 使用 useGeneratedKeys 回填）',
  `user_id`           int          NOT NULL                         COMMENT '所属用户 id，关联 mall_user.id',
  `receiver_name`     varchar(50)  NOT NULL                         COMMENT '收货人姓名',
  `receiver_phone`    varchar(20)  DEFAULT NULL                     COMMENT '收货人固定电话',
  `receiver_mobile`   varchar(20)  DEFAULT NULL                     COMMENT '收货人手机号',
  `receiver_province` varchar(50)  DEFAULT NULL                     COMMENT '省份',
  `receiver_city`     varchar(50)  DEFAULT NULL                     COMMENT '城市',
  `receiver_district` varchar(50)  DEFAULT NULL                     COMMENT '区/县',
  `receiver_address`  varchar(200) DEFAULT NULL                     COMMENT '详细地址',
  `receiver_zip`      varchar(20)  DEFAULT NULL                     COMMENT '邮政编码',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_shipping_user_id` (`user_id`) COMMENT '按用户查地址列表（selectByUid）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='收货地址表';

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
-- 【未纳入本脚本的表 · 说明（不猜）】
--   1) mall_pay_info（支付信息表）——**未创建**。
--      依据：它只出现在 generatorConfig.xml 第 43 行的**注释**里；本项目
--      src/main/java/com/imooc/mall/dao/ 下**没有** PayInfoMapper.java，
--      resources/mappers/ 下**没有** PayInfoMapper.xml，全项目检索不到任何
--      `mall_pay_info` 的 SQL 引用。同名的 com.imooc.mall.listenVo.PayInfo
--      是 MQ 支付消息的 DTO（PayMsgListener 用 Gson 反序列化消息体），
--      字段为 orderNo/payPlatform/platformNumber/platformStatus/payAmount，
--      **没有对应的 resultMap**，因此其列名无权威来源，本脚本不予臆造。
--      若后续要接入支付表，需拿到该服务的真实 DDL 再补。
--   2) 购物车表 ——**不存在**。Cart 存 Redis（CartServiceImpl：key=`cart_%d`，
--      hash 的 field=productId，value=Cart 的 JSON），不落库。
--
-- 【varchar 长度不确定项 · 明确列出】
--   所有 VARCHAR 列的长度在源码中均无依据（resultMap 只给 jdbcType=VARCHAR，
--   generatorConfig.xml 也未配置长度），下文长度纯属按语义给出的建议值。
--   其中尤其**不确定**的两列（长度差一个数量级都会影响可用性）：
--     · mall_product.detail    —— 可能是富文本 HTML 详情。若走 5.7 以前的
--                                 utf8mb4 单列 65535 字节上限附近可放宽到
--                                 varchar(6000) 或 text；本脚本取 varchar(2000)。
--     · mall_product.sub_images —— 逗号分隔的多图路径串，张数未知。
--   如需与真实库严格一致，请以真实库 SHOW CREATE TABLE 为准。
--
-- 脚本执行顺序/自检提示：6 张表与 6 个 mapper 的 resultMap 列集合已逐表比对，
--   无遗漏、无多余（比对结论见交付说明）。
-- =============================================================================
