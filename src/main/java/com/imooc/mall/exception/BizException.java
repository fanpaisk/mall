package com.imooc.mall.exception;

import com.imooc.mall.enums.ResponseEnum;

/**
 * 业务异常。
 *
 * <p>专门用于让 {@code @Transactional} 感知失败：方法抛 RuntimeException 时事务管理器才会回滚，
 * 而 {@code return ResponseVo.error(...)} 属于正常返回，代理会照常 commit。
 *
 * <p>异常最终由 RuntimeExceptionHandler 转成统一 JSON，前端拿到的仍是 {status, msg}。
 */
public class BizException extends RuntimeException {

    private final ResponseEnum responseEnum;

    public BizException(ResponseEnum responseEnum, String message) {
        super(message);
        this.responseEnum = responseEnum;
    }

    public ResponseEnum getResponseEnum() {
        return responseEnum;
    }
}
