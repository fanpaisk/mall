-- =============================================================================
-- 2026-11-16 密码算法 MD5 → BCrypt 的配套列宽升级
--
-- 背景：BCrypt 密文固定 60 个字符（$2a$10$ + 22 位盐 + 31 位摘要），
--       而 mall_user.password 原为 varchar(50)，装不下。
--
-- 不升级的后果：
--   · MySQL 严格模式：insert/update 直接报 Data too long for column 'password'
--   · 非严格模式：静默截断，截断后 PasswordUtils.matches 永远失败 → 用户登录不上
--
-- 执行（存量库）：mysql -u<user> -p mall < sql/upgrade-20261116-password-column.sql
-- 新库：直接执行 sql/mall.sql 即可（其中的 password 已是 varchar(100)）
-- =============================================================================

ALTER TABLE `mall_user`
    MODIFY COLUMN `password` varchar(100) NOT NULL
    COMMENT '密码密文：BCrypt 60 字符；历史 32 位 MD5 在登录成功后自动升级';
