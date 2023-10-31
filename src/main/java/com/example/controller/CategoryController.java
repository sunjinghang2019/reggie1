package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.domain.Category;
import com.example.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public Result<String> save(@RequestBody Category category){
        categoryService.save(category);
        return Result.success("添加成功");
    }

    @GetMapping("/page")
    public Result<Page> page(int page,int pageSize){
        log.info("page={},pageSize={}",page,pageSize);
        //使用Mp的分页插件,如果有name的话就构造条件构造器
        //1.分页构造器
        Page pageInfo = new Page(page,pageSize);//查第page页,查pageSize条件
        //2.条件构造器,添加过滤条件
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper();//注意指定泛型
        //3.保证查询数据的一致性,设置排序规则
        lambdaQueryWrapper.orderByDesc(Category::getSort);
        //4.执行查询(分页构造器,条件构造器)
        Page data = categoryService.page(pageInfo, lambdaQueryWrapper);
        return Result.success(data);
    }

    /**
     * 根据id删除分类
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result<String> deleteById(Long ids){
        log.info("删除分类,传到的id是{}",ids);
        //在分类管理列表页面，可以对某个分类进行删除，需要注意的是当分类关联了菜品或者套餐时，此分类不允许删除
        //一般不使用外键,外键影响性能
        categoryService.remove(ids);
        return Result.success("删除成功!");
    }

    /**
     * 根据id修改分类信息
     * @param category
     * @return
     */
    @PutMapping
    public Result<String> update(@RequestBody Category category){
        log.info("修改分类,得到的参数是{}",category.toString());
        categoryService.updateById(category);
        return Result.success("修改成功!");
    }


    /**
     * 获取菜品分配列表
     * @param type
     * @return
     */
    @GetMapping("/list")
    public Result<List<Category>> list(Integer type){
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(type!=null,Category::getType,type);
        lambdaQueryWrapper.orderByDesc(Category::getSort);
        lambdaQueryWrapper.orderByDesc(Category::getUpdateTime);
        List<Category> list = categoryService.list(lambdaQueryWrapper);
        return Result.success(list);
    }




}
