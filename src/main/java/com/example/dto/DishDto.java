package com.example.dto;

import com.example.domain.Dish;
import com.example.domain.DishFlavor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DishDto extends Dish {
    private List<DishFlavor> flavors = new ArrayList<>();//注意名称要一致

    private String categoryName;

    private Integer copies;
}
