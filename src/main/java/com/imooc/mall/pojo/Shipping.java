package com.imooc.mall.pojo;

import lombok.Data;

import java.util.Date;
/// 由于前端需要的字段与数据库提供的字段相同，所有不创建VO类
/// 直接返回数据库的实体类
@Data
public class Shipping {
    private Integer id;

    private Integer userId;

    private String receiverName;

    private String receiverPhone;

    private String receiverMobile;

    private String receiverProvince;

    private String receiverCity;

    private String receiverDistrict;

    private String receiverAddress;

    private String receiverZip;

    private Date createTime;

    private Date updateTime;

}