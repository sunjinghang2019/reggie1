package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.domain.Category;
import com.example.domain.Dish;
import com.example.domain.Setmeal;
import com.example.exception.CustomException;
import com.example.mapper.CategoryMapper;
import com.example.service.CategoryService;
import com.example.service.DishService;
import com.example.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper,Category> implements CategoryService {
    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id进行删除分类,删除之前需要进行判断
     * @param id
     */
    @Override
    public void remove(Long id) {
        //查询当前分类是否关联了菜品,如果已经关联,则抛出一个业务异常
        LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
        //添加查询条件
        dishWrapper.eq(Dish::getCategoryId,id);
        if(dishService.count(dishWrapper)>0){
            //已经关联菜品,需要抛出业务异常
            throw new CustomException("当前分类下关联了菜品,不能删除");
        }
        //查询当前分类是否关联了套餐,如果已经关联,则抛出一个业务异常
        LambdaQueryWrapper<Setmeal> setmealWrapper = new LambdaQueryWrapper<>();
        //添加查询条件
        setmealWrapper.eq(Setmeal::getCategoryId,id);
        if(setmealService.count(setmealWrapper)>0){
            //已经关联菜品,需要抛出业务异常
            throw new CustomException("当前分类下关联了套餐,不能删除");
        }
        //正常删除分类LambdaQueryWrapper
        LambdaUpdateWrapper<Category> categoryWrapper = new LambdaUpdateWrapper();
        categoryWrapper.set(Category::getIsDeleted,1);
        categoryWrapper.eq(Category::getId,id);
        //第一个参数要设置为null,这样就只会更新你set的字段
        super.update(null,categoryWrapper);
    }
}
