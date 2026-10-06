package com.imooc.mall.aspect;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect      // ① 声明：我是一个"质检站"（切面）
@Component   // ② 登记：把我交给 Spring 容器管理（IoC 那套）
@Slf4j
public class ApiLogAspect {

    private final Gson gson = new Gson();

    @Around("execution(* com.imooc.mall.controller.*.*(..))")   // ③ 切点：管住 controller 包下所有类的所有方法
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();               // ④ 进站：掐表
        Object result = pjp.proceed();                          // ⑤ 放行：执行真正的 Controller 方法
        long cost = System.currentTimeMillis() - start;        // ⑥ 出站：算耗时
        log.info("【接口耗时】{}ms 方法={} 参数={}", cost,
                pjp.getSignature().toShortString(), gson.toJson(pjp.getArgs()));
        return result;                                          // ⑦ 把业务结果原样交还
    }
}