package com.imooc.mall.form;

import lombok.Data;
import reactor.util.annotation.NonNull;

@Data
public class OrderCreateForm {
    @NonNull
    private Integer shippingId;
}
