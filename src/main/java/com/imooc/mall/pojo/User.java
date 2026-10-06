package com.imooc.mall.pojo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
public class User {
    private Integer id;

    private String username;

    private String password;

    private String email;

    private String phone;

    private String question;

    private String answer;

    private Integer role;

    private Date createTime;

    public User() {
    }

    private Date updateTime;

    public User(String username, String password, String email,Integer role) {
        this.role = role;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public User(@NotBlank String username, @NotBlank String password) {
        this.username = username;
        this.password = password;
    }
}