package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.domain.Dish;
import com.example.dto.DishDto;

import java.util.List;

public interface DishService extends IService<Dish> {
    /**
     * 保存页面传过来的口味数据
     * @param dishDto
     */
    void saveWithFlavor(DishDto dishDto);

    /**
     * 多表联查,将DTO结果给回页面渲染
     * @param id
     * @return
     */
    DishDto getIdWithFlavour(Long id);

    /**
     * 保存多个表的信息
     * @param dishDto
     */
    void updateWithFlavor(DishDto dishDto);

    /**
     * 根据id删除
     * @param ids
     */
    void deleteByIds(String ids);

    /**
     * 根据id启用或者禁用
     * @param ids
     */
    void changeStatusByIds(int status,String ids);

    /**
     * 根据菜品分类信息来获取所有的菜品
     * @param dish
     * @return
     */
    List<DishDto> getDishDtoListByCategory(Dish dish);

}
