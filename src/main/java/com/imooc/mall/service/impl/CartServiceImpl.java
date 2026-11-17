package com.imooc.mall.service.impl;

import com.github.pagehelper.util.StringUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements ICartService {

    /** 数量：field = 商品 id，value = 数字字符串，可用 HINCRBY 原子自增 */
    private static final String QTY_KEY_TEMPLATE = "cart_%d:qty";

    /** 勾选：field = 商品 id，value = 1 选中 / 0 未选中 */
    private static final String SEL_KEY_TEMPLATE = "cart_%d:sel";

    /** 购物车保留 7 天，每次写操作续期（滑动过期） */
    private static final long CART_TTL_DAYS = 7L;

    private static final String SELECTED = "1";
    private static final String UNSELECTED = "0";

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public ResponseVo<CartVo> add(Integer uid, CartAddForm form) {
        Integer productId = form.getProductId();
        Product product = productMapper.selectByPrimaryKey(productId);

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

        String qtyKey = qtyKey(uid);
        String selKey = selKey(uid);
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();

        // 一条 HINCRBY 完成“读-加一-写回”，原子执行，并发不会丢更新
        Long quantity = opsForHash.increment(qtyKey, String.valueOf(productId), 1L);

        // 返回 1 说明这个 field 是刚创建的：首次加车，补写勾选状态
        if (quantity != null && quantity == 1L) {
            opsForHash.put(selKey, String.valueOf(productId),
                    Boolean.TRUE.equals(form.getSelected()) ? SELECTED : UNSELECTED);
        }

        expireCart(qtyKey, selKey);
        return list(uid);
    }

    @Override
    public ResponseVo<CartVo> list(Integer uid) {
        String qtyKey = qtyKey(uid);
        String selKey = selKey(uid);
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();

        Map<String, String> qtyMap = opsForHash.entries(qtyKey);
        Map<String, String> selMap = qtyMap.isEmpty()
                ? Collections.<String, String>emptyMap()
                : opsForHash.entries(selKey);

        boolean selectAll = true;
        Integer cartTotalQuantity = 0;
        BigDecimal cartTotalPrice = BigDecimal.ZERO;
        List<CartProductVo> cartProductVoList = new ArrayList<>();

        if (!qtyMap.isEmpty()) {
            // Step 1: 一次 IN 查完所有商品（只查一次数据库）
            List<Integer> productIdList = qtyMap.keySet().stream()
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
            List<Product> productList = productMapper.selectByProductIdList(productIdList);

            // Step 2: 转成 Map<productId, Product>，便于 O(1) 组装
            Map<Integer, Product> productMap = productList.stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));

            // Step 3: 按商品 id 把“数量 + 勾选 + 商品信息”拼成一行
            for (Map.Entry<String, String> entry : qtyMap.entrySet()) {
                Integer productId = Integer.valueOf(entry.getKey());
                int quantity = Integer.parseInt(entry.getValue());
                boolean selected = SELECTED.equals(selMap.get(entry.getKey()));

                Product product = productMap.get(productId);
                if (product == null) {
                    // 下架/已删除的商品不展示，也不计入数量
                    continue;
                }

                CartProductVo cartProductVo = new CartProductVo(
                        productId,
                        quantity,
                        product.getName(),
                        product.getSubtitle(),
                        product.getMainImage(),
                        product.getPrice(),
                        product.getStatus(),
                        product.getPrice().multiply(BigDecimal.valueOf(quantity)),
                        product.getStock(),
                        selected
                );
                cartProductVoList.add(cartProductVo);

                if (!selected) {
                    selectAll = false;
                }
                //计算总价，只计算选中的
                if (selected) {
                    cartTotalPrice = cartTotalPrice.add(cartProductVo.getProductTotalPrice());
                }
                cartTotalQuantity += quantity;
            }
        }

        CartVo cartVo = new CartVo();
        cartVo.setSelectedAll(selectAll);
        cartVo.setCartTotalQuantity(cartTotalQuantity);
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setCartProductVoList(cartProductVoList);
        return ResponseVo.success(cartVo);
    }

    @Override
    public ResponseVo<CartVo> update(Integer uid, Integer productId, CartUpdateForm form) {
        String qtyKey = qtyKey(uid);
        String selKey = selKey(uid);
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();

        String value = opsForHash.get(qtyKey, String.valueOf(productId));
        if (StringUtil.isEmpty(value)) {
            //没有该商品,数据有问题
            return ResponseVo.error(ResponseEnum.CART_PRODUCT_NOT_EXIST);
        }

        if (form.getQuantity() != null && form.getQuantity() > 0) {
            opsForHash.put(qtyKey, String.valueOf(productId), String.valueOf(form.getQuantity()));
        }
        if (form.getSelected() != null) {
            opsForHash.put(selKey, String.valueOf(productId),
                    form.getSelected() ? SELECTED : UNSELECTED);
        }

        expireCart(qtyKey, selKey);
        return list(uid);
    }

    @Override
    public ResponseVo<CartVo> delete(Integer uid, Integer productId) {
        String qtyKey = qtyKey(uid);
        String selKey = selKey(uid);
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();

        String value = opsForHash.get(qtyKey, String.valueOf(productId));
        if (StringUtil.isEmpty(value)) {
            //没有该商品,数据有问题
            return ResponseVo.error(ResponseEnum.CART_PRODUCT_NOT_EXIST);
        }

        opsForHash.delete(qtyKey, String.valueOf(productId));
        opsForHash.delete(selKey, String.valueOf(productId));
        expireCart(qtyKey, selKey);
        return list(uid);
    }

    @Override
    public ResponseVo<CartVo> selectAll(Integer uid) {
        return markAll(uid, SELECTED);
    }

    @Override
    public ResponseVo<CartVo> unSelectAll(Integer uid) {
        return markAll(uid, UNSELECTED);
    }

    @Override
    public ResponseVo<Integer> sum(Integer uid) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        Map<String, String> qtyMap = opsForHash.entries(qtyKey(uid));
        int sum = 0;
        for (String quantity : qtyMap.values()) {
            sum += Integer.parseInt(quantity);
        }
        return ResponseVo.success(sum);
    }

    @Override
    public List<Cart> listForCart(Integer uid) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        Map<String, String> qtyMap = opsForHash.entries(qtyKey(uid));
        Map<String, String> selMap = qtyMap.isEmpty()
                ? Collections.<String, String>emptyMap()
                : opsForHash.entries(selKey(uid));

        List<Cart> cartList = new ArrayList<>();
        for (Map.Entry<String, String> entry : qtyMap.entrySet()) {
            Integer productId = Integer.valueOf(entry.getKey());
            boolean selected = SELECTED.equals(selMap.get(entry.getKey()));
            cartList.add(new Cart(productId, Integer.parseInt(entry.getValue()), selected));
        }
        return cartList;
    }

    /** 全选/取消全选：只改勾选 Hash，不动数量 */
    private ResponseVo<CartVo> markAll(Integer uid, String flag) {
        String selKey = selKey(uid);
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();

        Set<String> productIds = opsForHash.keys(qtyKey(uid));
        if (productIds != null && !productIds.isEmpty()) {
            Map<String, String> selMap = new HashMap<>();
            for (String productId : productIds) {
                selMap.put(productId, flag);
            }
            opsForHash.putAll(selKey, selMap);
            expireCart(qtyKey(uid), selKey);
        }
        return list(uid);
    }

    private String qtyKey(Integer uid) {
        return String.format(QTY_KEY_TEMPLATE, uid);
    }

    private String selKey(Integer uid) {
        return String.format(SEL_KEY_TEMPLATE, uid);
    }

    /** 写操作后续期，7 天不用自动清理 */
    private void expireCart(String... keys) {
        for (String key : keys) {
            redisTemplate.expire(key, CART_TTL_DAYS, TimeUnit.DAYS);
        }
    }
}
