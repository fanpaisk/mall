package com.imooc.mall.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 接口日志切面。
 *
 * <p>只记录方法名和耗时，不记录入参：登录表单里有 password 等敏感字段，
 * 序列化进日志等于明文泄漏。
 *
 * <p>用 try/catch/finally 保证异常路径也能记录耗时与堆栈；异常原样抛出，
 * 不影响 @Transactional 回滚和 @ControllerAdvice 的兜底处理。
 */
@Aspect
@Component
@Slf4j
public class ApiLogAspect {

    @Around("execution(* com.imooc.mall.controller.*.*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String method = pjp.getSignature().toShortString();
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            log.error("【接口异常】方法={}", method, e);
            throw e;
        } finally {
            log.info("【接口耗时】{}ms 方法={}", System.currentTimeMillis() - start, method);
        }
    }
}
