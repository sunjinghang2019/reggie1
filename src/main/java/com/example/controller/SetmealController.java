package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.domain.Dish;
import com.example.domain.Employee;
import com.example.domain.Setmeal;
import com.example.domain.SetmealDish;
import com.example.dto.SetmealDto;
import com.example.service.SetmealDishService;
import com.example.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;

    @GetMapping("/page")
    public Result<Page> page(int page, int pageSize,String name){
        log.info("接受到前端给的页面数据为:page:{},pageSize{}",page,pageSize);//数据接收到了
        Page data = setmealService.pageByPageInfo(page, pageSize, name);
        return Result.success(data);
    }

    @PostMapping
    @Transactional
    @CacheEvict(value = "setmealCache",allEntries = true)//allEntries = true代表删除这个分类下的所有数据
    public Result<String> save(@RequestBody SetmealDto setmealDto){
        log.info("所添加的套餐信息:{}",setmealDto);
        setmealService.saveWithDishes(setmealDto);
        return Result.success("添加成功!");
    }

    /**
     * 要做到回显的功能
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<SetmealDto> getById(@PathVariable Long id){
        SetmealDto setmealDto = setmealService.getWithDishesById(id);
        return Result.success(setmealDto);
    }

    @PutMapping
    @Transactional
    @CacheEvict(value = "setmealCache",allEntries = true)//allEntries = true代表删除这个分类下的所有数据
    public Result<String> updateWithDishes(@RequestBody SetmealDto setmealDto){
        setmealService.updateWithDishes(setmealDto);
        return Result.success("更新成功!");
    }

    /**
     *
     * @param status
     * @param ids
     * @return
     */
    @PostMapping ("/status/{status}")
    @CacheEvict(value = "setmealCache",allEntries = true)//allEntries = true代表删除这个分类下的所有数据
    public Result<String> changeStatus(@PathVariable int status,String ids){
        log.info("得到的状态是:{},得到的ids是:{}",status,ids);
        setmealService.changeStatusByIds(status,ids);
        return Result.success("修改状态成功!");
    }

    /**
     * 删除功能接口 单个删除&&批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache",allEntries = true)//allEntries = true代表删除这个分类下的所有数据
    public Result<String> deleteByIds(String ids){
        setmealService.deleteByIds(ids);
        return Result.success("删除成功!");
    }

    @GetMapping("/list")
    @Cacheable(value ="setmealCache",key = "#categoryId+'_'+#status")
    public Result<List<SetmealDto>> list(Long categoryId,int status){
        List list = setmealService.getByCategoryId(categoryId,status);
        return Result.success(list);
    }

    @GetMapping("/dish/{id}")
    public Result<List<SetmealDish>> getDishById(@PathVariable  Long id){
        return Result.success(setmealDishService.getSetmealDishListBySetmealId(id));
    }



}
