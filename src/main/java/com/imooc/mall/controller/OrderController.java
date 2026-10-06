package com.imooc.mall.controller;

import com.github.pagehelper.PageInfo;
import com.imooc.mall.comsts.MallConsts;
import com.imooc.mall.form.OrderCreateForm;
import com.imooc.mall.pojo.User;
import com.imooc.mall.service.IOrderService;
import com.imooc.mall.vo.OrderVo;
import com.imooc.mall.vo.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@RestController
public class OrderController {
    @Autowired
    private IOrderService orderService;
    @PostMapping("/orders")
    public ResponseVo<OrderVo> addOrder(@Valid @RequestBody OrderCreateForm form
                                        ,HttpSession session) {
        //uid从session里面取
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
        return orderService.create(user.getId(), form.getShippingId());
    }

    @GetMapping("/orders")
    public ResponseVo<PageInfo> list(@RequestParam Integer pageNum,
                                    @RequestParam Integer pageSize
                                    , HttpSession session) {
        //uid从session里面取
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
        return orderService.list(user.getId(), pageNum, pageSize);
    }

    @GetMapping("/orders/{orderNo}")
    public ResponseVo<OrderVo> detail(@PathVariable Long orderNo
                                    , HttpSession session) {
        //uid从session里面取
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
        return orderService.detail(user.getId(), orderNo);
    }

    @PutMapping("/orders/{orderNo}")
    public ResponseVo cancel(@PathVariable Long orderNo
                                     , HttpSession session) {
        //uid从session里面取
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
        return orderService.cancel(user.getId(), orderNo);
    }
}
