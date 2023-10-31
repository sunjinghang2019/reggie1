package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.MyStringHandler;
import com.example.common.Result;
import com.example.domain.Category;
import com.example.domain.Dish;
import com.example.domain.Employee;
import com.example.dto.DishDto;
import com.example.service.CategoryService;
import com.example.service.DishFlavorService;
import com.example.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public Result<String> save(@RequestBody DishDto dishDto){
        log.info("新增页面得到的数据{}",dishDto);
        dishService.saveWithFlavor(dishDto);
        return Result.success("新增菜品成功");
    }

    /**
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public Result<Page> page(int page, int pageSize,String name){
        //要注意的是,由于渲染数据还需要的菜品分类,但是我们Dish中只有菜品分类的id,而前端是无法获知我们菜品分类的id的
        //因此我们需要在拿到Dish之后,拿着这个菜品分类的id,去菜品分类表中去查你这个菜品分类的名称,拿到之后,再包装成DTO发送到前端即可
        //这里可以提一嘴,就是前端渲染数据的时候,拿到我们后端给它的json,会挨个地查询有没有一个属性名字叫做categoryName的,如果没有的话它就渲染不上
        //同样的,我们后端接收数据的话也就这样,它会去扫描前端给的json数据,挨个地查询有没有我们参数表里面的字段名,假如说有字段名的话,那么就会把数据填进去
        /**先拿到Dish**/
        log.info("page={},pageSize={}",page,pageSize);
        //使用Mp的分页插件,如果有name的话就构造条件构造器
        //1.分页构造器
        Page<Dish> pageInfo = new Page(page,pageSize);//查第page页,查pageSize条件
        Page<DishDto> pageDtoInfo = new Page<>();

        //2.条件构造器,添加过滤条件
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper();//注意指定泛型
        lambdaQueryWrapper.like(!StringUtils.isBlank(name), Dish::getName,name);//where name = ,注意当name为空的时候不搞这个sql
        //3.保证查询数据的一致性,设置排序规则
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        //4.执行查询(分页构造器,条件构造器)
        dishService.page(pageInfo, lambdaQueryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo,pageDtoInfo,"records");//records是正常的列表数据,不需要修改,我们的目的是修改categoryName
        List<Dish> records = pageInfo.getRecords();//然后咱根据records里面的数据id去获取名字
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);//设置普通属性值(Dish的值全部赋值进去)
            dishDto.setCategoryName(categoryService.getById(item.getCategoryId()).getName());//设置categoryName
            return dishDto;//完成后将该对象返回
        }).collect(Collectors.toList());

        pageDtoInfo.setRecords(list);
        return Result.success(pageDtoInfo);
    }

    /**
     * 删除功能接口 单个删除&&批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result<String> deleteByIds(String ids){
        dishService.deleteByIds(ids);
        return Result.success("删除成功!");
    }

    /**
     * 修改功能回显接口
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    //这个注解,什么要写成有el表达式的?什么时候不写成有el表达式的?
    //当参数是以url参数出现的也就是直接以/出现的,就要写成这样
    //当是以?分隔符连接达到,直接以参数表名出现即可
    public Result<DishDto> getDtoById(@PathVariable Long id){
        DishDto dto = dishService.getIdWithFlavour(id);
        return Result.success(dto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public Result<String> update(@RequestBody DishDto dishDto){
        log.info("新增页面得到的数据{}",dishDto);

        dishService.updateWithFlavor(dishDto);

        return Result.success("修改菜品成功");
    }


    /**
     *
     * @param status
     * @param ids
     * @return
     */
    @PostMapping ("/status/{status}")
    public Result<String> changeStatus(@PathVariable int status,String ids){
        log.info("得到的状态是:{},得到的ids是:{}",status,ids);
        dishService.changeStatusByIds(status,ids);
        return Result.success("修改状态成功!");
    }

    @GetMapping("/list")
    public Result<List<DishDto>> list(Dish dish){
        List<DishDto> listDto = dishService.getDishDtoListByCategory(dish);
        return Result.success(listDto);
    }









}
