package com.imooc.mall.dao;

import com.imooc.mall.pojo.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface ProductMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Product record);

    int insertSelective(Product record);

    Product selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Product record);

    int updateByPrimaryKey(Product record);

    List<Product> selectByCategoryIdSet(@Param("categoryIdSet") Set<Integer> categoryIdSet);

    List<Product> selectByProductIdSet(@Param("productIdSet") Set<Integer> productIdSet);

    List<Product> selectByProductIdList(@Param("productIdList") List<Integer> productIdList);

    /** 条件扣库存：库存够才扣，返回受影响行数（0 = 库存不足，并发安全） */
    int decreaseStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

}
