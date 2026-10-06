package com.imooc.mall.service.impl;

import com.imooc.mall.dao.CategoryMapper;
import com.imooc.mall.pojo.Category;
import com.imooc.mall.service.ICategoryService;
import com.imooc.mall.vo.CategoryVo;
import com.imooc.mall.vo.ResponseVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.imooc.mall.comsts.MallConsts.ROOT_PARENT_ID;

@Service
public class CategoryServiceImpl implements ICategoryService {
    @Autowired
    private CategoryMapper categoryMapper;

    /*
    耗时；
    http请求(请求微信api) > > 磁盘 > 内存
    mysql(内网+磁盘前期哇)
     */
    @Override
    public ResponseVo<List<CategoryVo>> selectAll() {
        List<CategoryVo> categoryVoList = new ArrayList<>();
        List<Category> categories = categoryMapper.selectAll();
        //查出parent_id=0的数据
        for (Category category : categories) {
            if (category.getParentId().equals(ROOT_PARENT_ID)){
                CategoryVo categoryVo = new CategoryVo();
                BeanUtils.copyProperties(category, categoryVo);
                categoryVoList.add(categoryVo);
            }
        }
        categoryVoList.sort(Comparator.comparing(CategoryVo::getSortOrder).reversed());
//        //TODO lambda + stream
//        categories.stream()
//                .filter(e -> e.getParentId().equals(ROOT_PARENT_ID))
//                .map(this::category2CategoryVo)
//                .collect(Collectors.toList());
        //查询子目录
        findSubCategories(categoryVoList, categories);

        return ResponseVo.success(categoryVoList);
    }

    @Override
    public void findSubCategoryId(Integer id, Set<Integer> resultSet) {
        List<Category> categories = categoryMapper.selectAll();
        findSubCategoryId(id, resultSet, categories);
    }

    public void findSubCategoryId(Integer id, Set<Integer> resultSet,List<Category> categories) {
        for (Category category : categories) {
            if (category.getParentId().equals(id)) {
                resultSet.add(category.getId());

                findSubCategoryId(category.getId(), resultSet);
            }
        }
    }


    ///查询子目录
    private void findSubCategories(List<CategoryVo> categoryVoList, List<Category> categories) {
        for (CategoryVo categoryVo : categoryVoList) {
            List<CategoryVo>subCategoryVoList = new ArrayList<>();
            for (Category category : categories) {
                //如果查到内容，设置subcategory，并继续往下查
                if (category.getParentId().equals(categoryVo.getId())) {
                    CategoryVo subCategoryVo = category2CategoryVo(category);
                    subCategoryVoList.add(subCategoryVo);
                }
                subCategoryVoList.sort(Comparator.comparing(CategoryVo::getSortOrder).reversed());
                categoryVo.setSubCategoryVo(subCategoryVoList);
                findSubCategories(subCategoryVoList, categories);
            }
        }

    }



    /// 将一条category装成返回对象categoryVo
    private CategoryVo category2CategoryVo(Category category){
        CategoryVo categoryVo = new CategoryVo();
        BeanUtils.copyProperties(category,categoryVo);
        return categoryVo;
    }
}
