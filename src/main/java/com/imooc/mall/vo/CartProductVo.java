package com.imooc.mall.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartProductVo {
    private Integer productid;

    private Integer quantity;

    private String productName;

    private String productSubtitle;

    private String productMainImage;

    private BigDecimal productPrice;

    private Integer productStatus;

    private BigDecimal productTotalPrice;
    //库存
    private Integer productStock;

    private Boolean productSelected;

    public CartProductVo(Integer productid, Integer quantity, String productName, String productSubtitle, String productMainImage, BigDecimal productPrice, Integer productStatus, BigDecimal productTotalPrice, Integer productStock, Boolean productSelected) {
        this.productid        = productid;
        this.quantity         = quantity;
        this.productName            = productName;
        this.productSubtitle         = productSubtitle;
        this.productMainImage       = productMainImage;
        this.productPrice        = productPrice;
        this.productStatus          = productStatus;
        this.productTotalPrice       = productTotalPrice;
        this.productStock       = productStock;
        this.productSelected        = productSelected;
    }
}
