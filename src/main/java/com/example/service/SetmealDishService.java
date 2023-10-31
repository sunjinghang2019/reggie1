package com.example.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.domain.SetmealDish;
import org.springframework.stereotype.Service;

import java.util.List;

public interface SetmealDishService  extends IService<SetmealDish> {
    List<SetmealDish> getSetmealDishListBySetmealId(Long id);
}
