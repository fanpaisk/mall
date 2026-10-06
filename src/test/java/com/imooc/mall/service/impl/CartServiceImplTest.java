package com.imooc.mall.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imooc.mall.MallConstsApplicationTests;
import com.imooc.mall.enums.ResponseEnum;
import com.imooc.mall.form.CartAddForm;
import com.imooc.mall.form.CartUpdateForm;
import com.imooc.mall.service.ICartService;
import com.imooc.mall.vo.CartVo;
import com.imooc.mall.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;
@Slf4j
public class CartServiceImplTest extends MallConstsApplicationTests {
    @Autowired
    private ICartService cartService;

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Integer uid = 1;

    private Integer productId = 26;

    @Before
    public void add() {
        log.info("【新增购物车...】");
        CartAddForm form = new CartAddForm();
        form.setProductId(27);
        form.setSelected(true);
        ResponseVo<CartVo> responseVo = cartService.add(1, form);
        log.info("list={}", gson.toJson(responseVo));
//        Assert.assertEquals(ResponseEnum.SUCCESS.getCode(), responseVo.getStatus());
    }

    @Test
    public void list() {
        ResponseVo<CartVo> list = cartService.list(1);
        log.info("list: {}", gson.toJson(list));
    }

    @Test
    public void update() {
        CartUpdateForm form = new CartUpdateForm();
        form.setQuantity(10);
        form.setSelected(true);
        ResponseVo<CartVo> responseVo = cartService.update(1,26,form);
        log.info("list: {}", gson.toJson(responseVo));
    }

    @After
    public void delete() {
        log.info("【删除购物车...】");
        ResponseVo<CartVo> responseVo = cartService.delete(1, 26);
        log.info("list: {}", gson.toJson(responseVo));
    }

    @Test
    public void selectAll() {
        ResponseVo<CartVo> responseVo = cartService.selectAll(1);
        log.info("list: {}", gson.toJson(responseVo));
    }
    @Test
    public void unSelectAll() {
        ResponseVo<CartVo> responseVo = cartService.unSelectAll(1);
        log.info("list: {}", gson.toJson(responseVo));
    }
    @Test
    public void sum() {
        ResponseVo<Integer> responseVo = cartService.sum(1);
        log.info("list: {}", gson.toJson(responseVo));
    }
}