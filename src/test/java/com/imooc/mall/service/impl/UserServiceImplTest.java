package com.imooc.mall.service.impl;

import com.imooc.mall.MallConstsApplicationTests;
import com.imooc.mall.enums.RoleEnum;
import com.imooc.mall.pojo.User;
import com.imooc.mall.service.IUserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

//这个注释是用于事务的，在测试里， 起到回滚作用
@Transactional
public class UserServiceImplTest extends MallConstsApplicationTests {
    @Autowired
    private IUserService userService;
    @Test
    public void register() {
        User user = new User("jack","123456","jack@qq.com", RoleEnum.CUSTOMER.getCode());
        userService.register(user);
    }
}