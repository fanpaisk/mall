package com.imooc.mall.pojo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Data
public class Category {
    private Integer id;

    private Integer parentId;

    private String name;

    private Boolean status;

    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;

}