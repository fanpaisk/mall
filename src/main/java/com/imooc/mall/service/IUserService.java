package com.imooc.mall.service;

import com.imooc.mall.pojo.User;
import com.imooc.mall.vo.ResponseVo;

/// 规范写法，先写接口后写实现类
public interface IUserService {
    /*
    注册的api
     */
    ResponseVo<User> register(User user);
    /*
    登录
     */
    ResponseVo<User> login(String username, String password);
    /*
    my商品类目
     */

}
