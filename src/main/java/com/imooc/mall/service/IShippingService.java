package com.imooc.mall.service;

import com.github.pagehelper.PageInfo;
import com.imooc.mall.form.ShippingForm;
import com.imooc.mall.vo.ResponseVo;
import org.springframework.stereotype.Service;

import java.util.Map;


public interface IShippingService {
    ResponseVo<Map<String,Integer>> add(Integer uid, ShippingForm Form);
    ResponseVo update(Integer uid, Integer id, ShippingForm Form);
    ResponseVo delete(Integer uid, Integer shippingId);
    //TODO 分页返回pageinfo对象，分页传参
    ResponseVo<PageInfo> list(Integer uid, Integer pageNum, Integer pageSize);
}
