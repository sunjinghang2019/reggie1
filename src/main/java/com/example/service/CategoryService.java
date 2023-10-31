package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.domain.Category;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;

public interface CategoryService extends IService<Category> {
    void remove(Long id);
}
