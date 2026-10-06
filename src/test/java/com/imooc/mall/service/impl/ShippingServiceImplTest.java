package com.imooc.mall.service.impl;

import com.github.pagehelper.PageInfo;
import com.imooc.mall.MallConstsApplicationTests;
import com.imooc.mall.enums.ResponseEnum;
import com.imooc.mall.form.ShippingForm;
import com.imooc.mall.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.ws.Action;

import java.util.Map;

import static org.junit.Assert.*;

@Slf4j
public class ShippingServiceImplTest extends MallConstsApplicationTests {

    @Autowired
    private ShippingServiceImpl shippingService;

    private Integer uid = 1;
    private ShippingForm shippingForm;
    private Integer shippingId=6;

    @Before
    public void before() {
        ShippingForm shippingForm = new ShippingForm();
        shippingForm.setReceiverName("范瑞华");
        shippingForm.setReceiverPhone("13823810249");
        shippingForm.setReceiverAddress("哈尔滨师范大学");
        shippingForm.setReceiverProvince("黑龙江");
        shippingForm.setReceiverCity("哈尔滨");
        shippingForm.setReceiverDistrict("南京路");
        this.shippingForm = shippingForm;
        add();
    }

    public void add() {
        ResponseVo<Map<String, Integer>> responseVo = shippingService.add(uid, shippingForm);
        log.info("日志responseVo={}", responseVo);
        this.shippingId=responseVo.getData().get("shippingId");
        Assert.assertEquals(ResponseEnum.SUCCESS.getCode(), responseVo.getStatus());
    }

    @After
    public void delete() {
        ResponseVo responseVo = shippingService.delete(uid, shippingId);
        log.info("日志responseVo={}", responseVo);
        Assert.assertEquals(ResponseEnum.SUCCESS.getCode(), responseVo.getStatus());
    }

    @Test
    public void update() {
        shippingForm.setReceiverName("施凯");
        ResponseVo responseVo = shippingService.update(uid, shippingId,shippingForm);
        log.info("日志responseVo={}", responseVo);
        Assert.assertEquals(ResponseEnum.SUCCESS.getCode(), responseVo.getStatus());
    }

    @Test
    public void list() {
        ResponseVo responseVo = shippingService.list(uid, 1,10);
        log.info("日志responseVo={}", responseVo);
        Assert.assertEquals(ResponseEnum.SUCCESS.getCode(), responseVo.getStatus());
    }
}