package com.imooc.mall.util;

import org.junit.Test;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * 纯单元测试：不依赖 Spring 容器、MySQL、Redis，可以直接跑。
 */
public class PasswordUtilsTest {

    @Test
    public void bcrypt_同一个明文两次密文不同() {
        String first = PasswordUtils.encode("123456");
        String second = PasswordUtils.encode("123456");
        assertNotEquals(first, second);
        assertTrue(first.startsWith("$2"));
    }

    @Test
    public void bcrypt_正确密码通过_错误密码拒绝() {
        String stored = PasswordUtils.encode("123456");
        assertTrue(PasswordUtils.matches("123456", stored));
        assertFalse(PasswordUtils.matches("1234567", stored));
        assertFalse(PasswordUtils.isLegacy(stored));
    }

    @Test
    public void legacy_md5_仍然可以通过校验() {
        String md5 = DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8));
        assertTrue(PasswordUtils.isLegacy(md5));
        assertTrue(PasswordUtils.matches("123456", md5));
        assertFalse(PasswordUtils.matches("1234567", md5));
    }

    @Test
    public void 空值不抛异常() {
        assertFalse(PasswordUtils.matches(null, "x"));
        assertFalse(PasswordUtils.matches("x", null));
    }
}
