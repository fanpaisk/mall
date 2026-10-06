package com.imooc.mall.controller;

import com.imooc.mall.comsts.MallConsts;
import com.imooc.mall.form.CartAddForm;
import com.imooc.mall.form.CartUpdateForm;
import com.imooc.mall.pojo.User;
import com.imooc.mall.service.ICartService;
import com.imooc.mall.vo.CartVo;
import com.imooc.mall.vo.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@RestController
public class CartController {
    @Autowired
    private ICartService cartService;

    @PostMapping("/carts")
    public ResponseVo<CartVo> add(@Valid @RequestBody CartAddForm cartAddForm,
                                  HttpSession session) {
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
        return cartService.add(user.getId(), cartAddForm);
    }

    @GetMapping("/carts")
    public ResponseVo<CartVo> list(HttpSession session) {
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
        return cartService.list(user.getId());
    }
    @PutMapping("/carts/{productId}")
    public ResponseVo<CartVo> update(@PathVariable Integer productId,
            @Valid @RequestBody CartUpdateForm cartUpdateForm,
                                     HttpSession session) {
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
        return cartService.update(user.getId(), productId,cartUpdateForm);
    }
    @DeleteMapping("/carts/{productId}")
    public ResponseVo<CartVo> delete(@PathVariable Integer productId,
            HttpSession session) {
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
        return cartService.delete(user.getId(), productId);
    }
    @PutMapping("/carts/selectAll")
    public ResponseVo<CartVo> selectAll(HttpSession session) {
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
        return cartService.selectAll(user.getId());
    }
    @PutMapping("/carts/unSelectAll")
    public ResponseVo<CartVo> unSelectAll(HttpSession session) {
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
        return cartService.unSelectAll(user.getId());
    }
    @GetMapping("/carts/products/sum")
    public ResponseVo<Integer> sum(HttpSession session) {
        User user = (User) session.getAttribute(MallConsts.CURRENT_USER);
        return cartService.sum(user.getId());
    }
}
