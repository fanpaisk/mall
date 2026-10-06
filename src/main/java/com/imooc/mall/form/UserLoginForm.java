package com.imooc.mall.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserLoginForm {
    /*
    这些注释是表单校验
 */
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
