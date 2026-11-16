package com.imooc.mall.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码加密与校验。
 *
 * <p>新数据统一用 BCrypt：每次加密随机生成盐，密文形如 <code>$2a$10$...</code>，
 * 盐和 cost 都包含在密文里，同一个明文每次得到的密文都不同，彩虹表失效。
 *
 * <p>历史数据是 32 位无盐 MD5。为了不把老用户挡在门外，{@link #matches} 同时兼容两种格式；
 * 老用户登录成功后由 UserServiceImpl 透明升级为 BCrypt。
 */
public final class PasswordUtils {

    /** Spring Security 的 BCrypt 实现，默认 cost=10（约 2^10 轮，单次几十毫秒）。 */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /** BCrypt 密文的固定前缀（$2a$ / $2b$ / $2y$）。 */
    private static final String BCRYPT_PREFIX = "$2";

    /** MD5 十六进制密文固定 32 个字符。 */
    private static final int MD5_HEX_LENGTH = 32;

    private PasswordUtils() {
    }

    /** 生成 BCrypt 密文，随机盐已包含在返回值中。 */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /** 判断库里的密文是否是需要升级的历史 MD5。 */
    public static boolean isLegacy(String storedPassword) {
        return storedPassword != null
                && storedPassword.length() == MD5_HEX_LENGTH
                && !storedPassword.startsWith(BCRYPT_PREFIX);
    }

    /** 校验明文与库中密文：BCrypt 走强哈希，历史 MD5 走兼容分支。 */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith(BCRYPT_PREFIX)) {
            return ENCODER.matches(rawPassword, storedPassword);
        }
        String md5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
        return md5.equalsIgnoreCase(storedPassword);
    }
}
