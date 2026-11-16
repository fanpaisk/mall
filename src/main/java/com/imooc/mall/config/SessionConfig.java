package com.imooc.mall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Spring Session 的序列化方式：JSON，而不是默认的 JDK 序列化。
 *
 * <p>JDK 序列化要求 Session 里每个对象都实现 Serializable，且类字段一旦变化，
 * 旧 Session 反序列化就会失败；JSON 只存数据本身，字段增减更平滑，Redis 里也看得懂。
 *
 * <p>Bean 名必须是 springSessionDefaultRedisSerializer，Spring Boot 的
 * RedisSessionConfiguration 按名字查找它。
 */
@Configuration
public class SessionConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
}
