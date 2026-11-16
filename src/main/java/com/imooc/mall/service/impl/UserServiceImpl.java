package com.imooc.mall.service.impl;

import com.imooc.mall.dao.UserMapper;
import com.imooc.mall.enums.ResponseEnum;
import com.imooc.mall.enums.RoleEnum;
import com.imooc.mall.pojo.User;
import com.imooc.mall.service.IUserService;
import com.imooc.mall.util.PasswordUtils;
import com.imooc.mall.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public ResponseVo<User> register(User user) {



        //校验username，email不能重复
        int countByUsername = userMapper.countByUsername(user.getUsername());
        if (countByUsername > 0) {
            return ResponseVo.error(ResponseEnum.USERNAME_EXIST);
        }
        int countByEmail = userMapper.countByEmail(user.getEmail());
        if (countByEmail > 0) {
            return ResponseVo.error(ResponseEnum.EMAIL_EXIST);
        }

        user.setRole(RoleEnum.CUSTOMER.getCode());

        // 密码加密：BCrypt，随机盐 + 可调 cost；同一个明文每次密文都不同
        user.setPassword(PasswordUtils.encode(user.getPassword()));

        //写入数据库
        int resultCount = userMapper.insertSelective(user);
        if (resultCount == 0) {
            return ResponseVo.error(ResponseEnum.ERROR);
        }
        return ResponseVo.success();
    }

    @Override
    public ResponseVo<User> login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            //用户不存在(返回用户名和密码错误)
            return ResponseVo.error(ResponseEnum.USERNAME_OR_PASSWORD_ERROR);
        }

        // 校验：新数据走 BCrypt，历史数据（32 位无盐 MD5）由 PasswordUtils 自动兼容
        if (!PasswordUtils.matches(password, user.getPassword())) {
            //密码错误(返回用户名和密码错误)
            return ResponseVo.error(ResponseEnum.USERNAME_OR_PASSWORD_ERROR);
        }

        // 老用户登录成功后，把 MD5 密文透明升级为 BCrypt；失败不影响本次登录
        if (PasswordUtils.isLegacy(user.getPassword())) {
            upgradePassword(user.getId(), password);
        }

        user.setPassword("");
        return ResponseVo.success(user);
    }

    /** 平滑迁移：只更新 id + password 两个字段，避免整行覆盖。 */
    private void upgradePassword(Integer userId, String rawPassword) {
        try {
            User upgrade = new User();
            upgrade.setId(userId);
            upgrade.setPassword(PasswordUtils.encode(rawPassword));
            userMapper.updateByPrimaryKeySelective(upgrade);
            log.info("用户 {} 的密码已从 MD5 升级为 BCrypt", userId);
        } catch (Exception e) {
            // 升级只是顺带优化，不能因为它让用户登不进来
            log.warn("用户 {} 的密码升级失败，下次登录再试：{}", userId, e.getMessage());
        }
    }

    private void error(){throw new RuntimeException("意外错误");}


}
