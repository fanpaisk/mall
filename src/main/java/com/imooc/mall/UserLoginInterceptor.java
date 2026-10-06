package com.imooc.mall;

import com.imooc.mall.comsts.MallConsts;
import com.imooc.mall.exception.UserLoginException;
import com.imooc.mall.pojo.User;
import com.imooc.mall.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.omg.CORBA.UserException;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.imooc.mall.enums.ResponseEnum.NEED_LOGIN;

/// 这个类只是写了拦截器，InterceptorConfig才是真正用了拦截器
@Slf4j
public class UserLoginInterceptor implements HandlerInterceptor {
    /*
    true表示继续流程，false表示中断
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("preHandle...");

        User user = (User) request.getSession().getAttribute(MallConsts.CURRENT_USER);
        if (user == null) {
            log.info("user is null");
            /// 这里抛异常自然会在我写的异常类被捕获
            throw new UserLoginException();
//            return false;
//            return ResponseVo.error(NEED_LOGIN);
        }
        return true;
    }
}
