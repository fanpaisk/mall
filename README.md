# mall —— 商城系统（订单主服务）

与支付服务 [`pay`](https://github.com/fanpaisk/pay) 共同组成一个完整的商城系统：
本仓库是**订单侧主服务**（用户、商品、购物车、订单），支付能力单独部署在 `pay`，
两者是**两个独立进程**，之间**没有 HTTP 直连**，而是通过 **RabbitMQ** 异步解耦。

> 配套的支付服务：**https://github.com/fanpaisk/pay**（统一下单 / 回调验签 / 金额校验 / 幂等）

## 技术栈

| 项 | 值 |
|---|---|
| 框架 | Spring Boot 2.1.7 / Java 8 / SpringMVC |
| 持久层 | MyBatis + MySQL + PageHelper（分页） |
| 缓存 | Redis（购物车、用户信息） |
| 消息 | RabbitMQ（消费 `payNotify` 更新订单状态） |
| 其他 | AOP（接口耗时日志）、Lombok、Gson |

## 功能模块

- **用户**：注册、登录（Session + 拦截器）、邮箱/用户名唯一校验
- **商品**：分类浏览、商品列表与详情、后台上下架
- **购物车**：Redis Hash 存储，选中/全选、数量增减、合计金额
- **订单**：下单（校验地址归属 → 扣库存 → 写订单主表与明细）、订单列表与详情、取消
- **支付对接**：调用 `pay` 服务下单，消费 `payNotify` 队列把订单置为已支付
- **运维**：`@Aspect + @Around` 统计 Controller 接口耗时与入参

## 下单 → 支付 主链路

```
用户 → POST /orders（登录态由拦截器校验）
        └─ OrderServiceImpl.create  【@Transactional】
             ├─ 校验收货地址归属（shippingMapper.selectByUidAndShippingId）
             ├─ 取购物车选中项（Redis Hash: cart_{uid}）
             ├─ selectByProductIdSet 一次 IN 查商品
             ├─ 循环校验「存在 / 在售 / 库存足」并逐条扣减库存
             ├─ orderMapper.insertSelective + orderItemMapper.batchInsert
             └─ 循环删除已下单的购物车项

用户 → pay 服务 /pay/create 生成微信 Native 二维码 → 扫码支付
微信 → pay 服务 /pay/notify（验签 → 查单 → 金额校验 → 幂等 → 更新支付记录）
        └─ amqpTemplate.convertAndSend("payNotify", json)
                └─ 本服务 PayMsgListener（@RabbitListener(queues="payNotify")）
                        └─ OrderServiceImpl.paid(orderNo) → 订单置 PAID + paymentTime
```

**为什么用 MQ 而不是支付成功后直接 HTTP 调订单服务**：微信回调在意的是"尽快收到"，同步 HTTP 会让回调被下游拖慢甚至触发微信重推；订单服务临时不可用时消息还在队列里，重启后照样消费；天然削峰。代价是**最终一致**，所以双方都要幂等。

## 目录结构

```
src/main/java/com/imooc/mall/
├── controller/      # UserController / CartController / OrderController / ProductController ...（11 个）
├── service/impl/    # 业务实现（其中 OrderServiceImpl / CartServiceImpl 是核心）
├── dao/             # MyBatis Mapper 接口
├── pojo/ vo/ form/  # 实体 / 视图对象 / 入参对象
├── config/          # WebMvc、Redis、拦截器注册等
├── aspect/          # ApiLogAspect（@Around 统计耗时）
├── listener/        # PayMsgListener（消费 payNotify）
├── exception/       # RuntimeExceptionHandler（@ControllerAdvice 全局异常）
└── comsts/          # 常量
src/main/resources/
├── application.yml           # 敏感项全部走环境变量占位符
├── mappers/*.xml             # 6 张表的 SQL 映射
└── generatorConfig.xml.example  # MyBatis Generator 脱敏示例（真实文件已 gitignore）
sql/mall.sql                  # 建表脚本（本次整理补入）
```

## 快速开始

1. **建库建表**：执行 `sql/mall.sql`（默认库名 `mall`）——含 用户 / 分类 / 商品 / 订单 / 订单明细 / 收货地址 **6 张表**。
   支付记录表 `mall_pay_info` 属于支付服务，脚本在 [`pay/sql/pay_info.sql`](https://github.com/fanpaisk/pay/blob/main/sql/pay_info.sql)（两个服务**共用同一个库**）。
2. **中间件**：MySQL + Redis + RabbitMQ。注意 `pay` 服务发消息用的 `payNotify` 队列**需要预先存在**。
3. **配置**：敏感项通过环境变量注入（见下表），或把真实值写进 `src/main/resources/application-local.yml`
   （已 gitignore）并以 `--spring.profiles.active=local` 启动。
4. **启动**：`mvn spring-boot:run`（默认端口 8081；支付服务为 8083）。

### 配置项

| 环境变量 | 说明 | 默认值 |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | 数据库连接 | `jdbc:mysql://localhost:3306/mall?...` / `root` / 空 |
| `RABBITMQ_HOST` / `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | RabbitMQ | `127.0.0.1` / `guest` / `guest` |
| （Redis） | 见 `application.yml` 的 `spring.redis` | `127.0.0.1:6379` |

## 已知限制与可改进点

诚实记录当前实现的边界：

1. **扣库存存在超卖风险**：先 `SELECT` 出库存在内存里比较、再 `update` 回写，中间没有行锁或版本号；并发下两人可同时判定库存足够。正确做法是条件更新 `update mall_product set stock = stock - #{qty} where id = #{id} and stock >= #{qty}`，看影响行数判断是否抢到；或用乐观锁版本号。
2. **`@Transactional` 的回滚边界有漏洞**：`OrderServiceImpl.create` 中途校验失败是 `return ResponseVo.error(...)`，方法正常返回、事务不会回滚，前面已扣的库存会留在库里。应改为抛业务异常（由全局异常处理器映射回原错误码），或用 `setRollbackOnly()`。
3. **Redis 操作不在数据库事务内**：购物车删除虽然写在事务方法里，但 Redis 不受 `DataSourceTransactionManager` 管辖，MySQL 回滚了 Redis 也不会回滚；当前是靠"把删购物车放在最后"做顺序补偿。
4. **消息消费者吞异常**：`PayMsgListener` 捕获异常后只打日志不外抛，配合 Spring AMQP 默认自动 ack，会导致这条支付通知永久丢失、订单一直待支付。应让它抛出以触发重试，并配置 `default-requeue-rejected: false` + 死信队列人工兜底。
5. **`payNotify` 队列未在代码中声明**：两边都只用字符串常量，队列需预先创建。
6. **Session 存在单机内存**：多实例部署会出现 A 机登录、B 机不认；改进方向是 Spring Session + Redis 或改无状态 JWT。
7. **密码使用 MD5 存储**（无盐），应换成 BCrypt。
8. **测试缺少断言**：`src/test` 下的测试依赖真实 MySQL/Redis/RabbitMQ，且以日志打印为主，不能作为回归保障。

## 声明

初始代码骨架来自慕课网《SpringBoot 商城》课程；本仓库为**补全与二次开发版本**，
其中脱敏改造（敏感配置全部改为环境变量注入、补充建表脚本 `sql/mall.sql` 与本文档）
以及上述改进点的整理为二次开发内容。
