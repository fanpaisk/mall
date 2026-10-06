package com.imooc.mall.service.impl;

import com.github.pagehelper.util.StringUtil;
import com.google.gson.Gson;
import com.imooc.mall.dao.ProductMapper;
import com.imooc.mall.enums.ProductStatusEnum;
import com.imooc.mall.enums.ResponseEnum;
import com.imooc.mall.form.CartAddForm;
import com.imooc.mall.form.CartUpdateForm;
import com.imooc.mall.pojo.Cart;
import com.imooc.mall.pojo.Product;
import com.imooc.mall.service.ICartService;
import com.imooc.mall.vo.CartProductVo;
import com.imooc.mall.vo.CartVo;
import com.imooc.mall.vo.ResponseVo;
import com.mysql.cj.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements ICartService {
    private final static String CART_REDIS_KEY_TEMPLATE = "cart_%d";
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    private Gson gson = new Gson();

    @Override
    public ResponseVo<CartVo> add(Integer uid, CartAddForm form) {
        Integer quantity = 1;
        Product product = productMapper.selectByPrimaryKey(form.getProductId());

        //判断商品是否存在
        if (product == null) {
            return ResponseVo.error(ResponseEnum.PRODUCT_NOT_EXIST);
        }
        //商品是否是正常的在售状态
        if (!product.getStatus().equals(ProductStatusEnum.ON_SALE.getCode())) {
            return ResponseVo.error(ResponseEnum.PRODUCT_OFF_SALE_OR_DELETE);
        }
        //商品库存是否充足
        if (product.getStock() <= 0) {
            return ResponseVo.error(ResponseEnum.PRODUCT_STOCK_ERROR);
        }

        //TODO 更新到redis
        //key:cart_1
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        String redisKey = String.format(CART_REDIS_KEY_TEMPLATE, uid);
        String value = opsForHash.get(redisKey, String.valueOf(product.getId()));
        Cart cart;
        if (StringUtil.isEmpty(value)) {
            //没有该商品,新增
            cart = new Cart(product.getId(), quantity, form.getSelected());
        } else {
            //已经有了，数量要加1
            cart = gson.fromJson(value, Cart.class);
            cart.setQuantity(cart.getQuantity() + quantity);
        }
        opsForHash.put(redisKey,
                String.valueOf(product.getId()),
                gson.toJson(cart));

        return list(uid);
    }

    @Override
    public ResponseVo<CartVo> list(Integer uid) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        String redisKey = String.format(CART_REDIS_KEY_TEMPLATE, uid);
        Map<String, String> entries = opsForHash.entries(redisKey);

        boolean selectAll = true;
        Integer cartTotalQuantity = 0;
        BigDecimal cartTotalPrice = BigDecimal.ZERO;

        CartVo cartVo = new CartVo();
        List<CartProductVo> cartProductVoList = new ArrayList<>();
//        for (Map.Entry<String,String> entry:entries.entrySet()){
//            Integer productId = Integer.valueOf(entry.getKey());
//            Cart cart = gson.fromJson(entry.getValue(), Cart.class);
//            //TODO需要优化，不要在for里面查sql，使用mysql里面的in
//            Product product = productMapper.selectByPrimaryKey(productId);
//            if (product != null) {
//                CartProductVo cartProductVo = new CartProductVo(
//                        productId,
//                        cart.getQuantity(),
//                        product.getName(),
//                        product.getSubtitle(),
//                        product.getMainImage(),
//                        product.getPrice(),
//                        product.getStatus(),
//                        product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())),
//                        product.getStock(),
//                        cart.getProductSelected()
//                );
//                cartProductVoList.add(cartProductVo);
//
//                if (!cart.getProductSelected())
//                {
//                    selectAll = false;
//                }
//                //计算总价，只计算选中的
//                if (cart.getProductSelected()) {
//                    cartTotalPrice = cartTotalPrice.add(cartProductVo.getProductTotalPrice());
//                }
//            }
//            cartTotalQuantity += cart.getQuantity();
//        }
        // Step 1: 提取所有 productId
        List<Integer> productIdList = new ArrayList<>();
        Map<Integer, Cart> cartMap = new HashMap<>(); // 保存 productId -> Cart 的映射

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            Integer productId = Integer.valueOf(entry.getKey());
            Cart cart = gson.fromJson(entry.getValue(), Cart.class);
            productIdList.add(productId);
            cartMap.put(productId, cart); // 缓存 cart 数据
        }

        // Step 2: 批量查询所有商品（只查一次数据库）
        if (!productIdList.isEmpty()) {
            List<Product> productList = productMapper.selectByProductIdList(productIdList);

            // Step 3: 转换为 Map<productId, Product>，便于快速查找
            Map<Integer, Product> productMap = productList.stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));

            // Step 4: 遍历所有 productId，构建 CartProductVo
            for (Integer productId : productIdList) {
                Cart cart = cartMap.get(productId);
                Product product = productMap.get(productId);

                if (product != null) {
                    CartProductVo cartProductVo = new CartProductVo(
                            productId,
                            cart.getQuantity(),
                            product.getName(),
                            product.getSubtitle(),
                            product.getMainImage(),
                            product.getPrice(),
                            product.getStatus(),
                            product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())),
                            product.getStock(),
                            cart.getProductSelected()
                    );
                    cartProductVoList.add(cartProductVo);

                    if (!cart.getProductSelected()) {
                        selectAll = false;
                    }
                    //计算总价，只计算选中的
                    if (cart.getProductSelected()) {
                        cartTotalPrice = cartTotalPrice.add(cartProductVo.getProductTotalPrice());
                    }
                }
                cartTotalQuantity += cart.getQuantity();
            }
        }
        cartVo.setSelectedAll(selectAll);
        cartVo.setCartTotalQuantity(cartTotalQuantity);
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setCartProductVoList(cartProductVoList);
        return ResponseVo.success(cartVo);
    }

    @Override
    public ResponseVo<CartVo> update(Integer uid, Integer productId, CartUpdateForm form) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        String redisKey = String.format(CART_REDIS_KEY_TEMPLATE, uid);
        String value = opsForHash.get(redisKey, String.valueOf(productId));
        if (StringUtil.isEmpty(value)) {
            //没有该商品,数据有问题
            return ResponseVo.error(ResponseEnum.CART_PRODUCT_NOT_EXIST);
        }
        //已经有了，修改内容
        Cart cart = gson.fromJson(value, Cart.class);
        if (form.getQuantity() != null
                && form.getQuantity() > 0) {
            cart.setQuantity(form.getQuantity());
        }
        if (form.getSelected() != null) {
            cart.setProductSelected(form.getSelected());
        }
        /// 将购物车更新入redis
        opsForHash.put(redisKey, String.valueOf(productId), gson.toJson(cart));
        return list(uid);
    }

    @Override
    public ResponseVo<CartVo> delete(Integer uid, Integer productId) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        String redisKey = String.format(CART_REDIS_KEY_TEMPLATE, uid);
        String value = opsForHash.get(redisKey, String.valueOf(productId));
        if (StringUtil.isEmpty(value)) {
            //没有该商品,数据有问题
            return ResponseVo.error(ResponseEnum.CART_PRODUCT_NOT_EXIST);
        }
        //已经有了，修改内容
        /// 将购物车更新入redis
        opsForHash.delete(redisKey, String.valueOf(productId));
        return list(uid);
    }

    @Override
    public ResponseVo<CartVo> selectAll(Integer uid) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        String redisKey = String.format(CART_REDIS_KEY_TEMPLATE, uid);

        for (Cart cart : listForCart(uid)) {
            cart.setProductSelected(true);
            opsForHash.put(redisKey, String.valueOf(cart.getProductId()), gson.toJson(cart));
        }
        return list(uid);
    }

    @Override
    public ResponseVo<CartVo> unSelectAll(Integer uid) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        String redisKey = String.format(CART_REDIS_KEY_TEMPLATE, uid);

        for (Cart cart : listForCart(uid)) {
            cart.setProductSelected(false);
            opsForHash.put(redisKey, String.valueOf(cart.getProductId()), gson.toJson(cart));
        }
        return list(uid);
    }

    @Override
    public ResponseVo<Integer> sum(Integer uid) {
        Integer sum = listForCart(uid).stream()
                .map(Cart::getQuantity)
                .reduce(0, Integer::sum);
        return ResponseVo.success(sum);
    }

    public List<Cart> listForCart(Integer uid) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        String redisKey = String.format(CART_REDIS_KEY_TEMPLATE, uid);
        Map<String, String> entries = opsForHash.entries(redisKey);

        List<Cart> cartList = new ArrayList<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            cartList.add(gson.fromJson(entry.getValue(), Cart.class));
        }
        return cartList;
    }
}

