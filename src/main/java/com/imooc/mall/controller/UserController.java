package com.imooc.mall.controller;

import com.imooc.mall.comsts.MallConsts;
import com.imooc.mall.enums.ResponseEnum;
import com.imooc.mall.form.UserLoginForm;
import com.imooc.mall.form.UserRegisterForm;
import com.imooc.mall.pojo.User;
import com.imooc.mall.service.IUserService;
import com.imooc.mall.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import java.util.Objects;

import static com.imooc.mall.enums.ResponseEnum.*;

@Slf4j
@RestController
public class UserController {
    @Autowired
    private IUserService userService;

    @PostMapping("/user/register")
//    private void register(@RequestParam String username) {
//        log.info("username is {}", username);
//    }
    private ResponseVo register(@Valid @RequestBody UserRegisterForm userRegisterForm
//            , BindingResult bindingResult
    ) {
        /// BindingResult bindingResult用于表单验证
        /// 表单验证，是否存在值
//        if (bindingResult.hasErrors()) {
//            log.error("注册提交的参数有误,{} {}",
//                    Objects.requireNonNull(bindingResult.getFieldError()).getField(),
//                    bindingResult.getFieldError().getDefaultMessage());
//            return ResponseVo.error(PARAM_ERROR, bindingResult);
//        }

        User user = new User();
        BeanUtils.copyProperties(userRegisterForm, user);
        return userService.register(user);
    }

    @PostMapping("/user/login")
    private ResponseVo login(@Valid @RequestBody UserLoginForm userLoginForm,
//                             BindingResult bindingResult,
                             HttpSession session) {
        /// BindingResult bindingResult用于表单验证
        /// 表单验证，是否存在值
//        if (bindingResult.hasErrors()) {
//            return ResponseVo.error(PARAM_ERROR, bindingResult);
//        }

        ResponseVo<User> userResponseVo = userService.login(userLoginForm.getUsername(), userLoginForm.getPassword());
        //设置Session
        session.setAttribute(MallConsts.CURRENT_USER, userResponseVo.getData());

        return userResponseVo;
    }

    //TODO session保存在内存里，容易丢失。故改进版redis+token
    @GetMapping("/user")
    public ResponseVo<User> userInfo(HttpSession session) {
        ///判断是否是登录状态，
        /*
        这里面经过
 1. 用户请求 GET /user
   ↓
2. 浏览器自动带上 Cookie：
   → Cookie: JSESSIONID=abc123xyz
   ↓
3. 服务器收到请求，根据 JSESSIONID 找到对应的 HttpSession 对象
   ↓
4. Spring 把这个 HttpSession 注入到方法参数中
   → HttpSession session
   ↓
5. 你从 session 中取用户对象：
   → session.getAttribute("CURRENT_USER")
   ↓
6. 如果取到 → 已登录；取不到 → 未登录
         */
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
//        if (user == null) {
//            return ResponseVo.error(NEED_LOGIN);
//        }

        return ResponseVo.success(user);
    }

    //TODO 判断登录状态,拦截器
    @PostMapping("/user/logout")
    public ResponseVo logout(HttpSession session) {
        log.info("/user/logout sessionId:{}", session.getId());
        /// 要登出先判断是否是登录状态
//        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
//        if (user == null) {
//            return ResponseVo.error(NEED_LOGIN);
//        }

        session.removeAttribute(MallConsts.CURRENT_USER);
        return ResponseVo.success();
    }
}
