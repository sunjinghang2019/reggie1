package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.domain.Setmeal;
import com.example.dto.SetmealDto;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    /**
     * 增加套餐的方法
     * @param setmealDto
     */
    void saveWithDishes(SetmealDto setmealDto);

    /**
     * 得到套餐的完整信息功能
     * @param id
     * @return
     */
    SetmealDto getWithDishesById(Long id);

    /**
     * 根据页面传过来的数据更新
     * @param setmealDto
     */
    void updateWithDishes(SetmealDto setmealDto);

    void changeStatusByIds(int status,String ids);

    void deleteByIds(String ids);

    Page pageByPageInfo(int page,int pageSize,String name);

    List<Setmeal> getByCategoryId(Long categoryId,int status);
}
