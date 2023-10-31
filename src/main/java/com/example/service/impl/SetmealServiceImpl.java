package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.CustomIdentifierGenerator;
import com.example.common.MyStringHandler;
import com.example.domain.*;
import com.example.dto.SetmealDto;
import com.example.mapper.SetmealMapper;
import com.example.service.CategoryService;
import com.example.service.SetmealDishService;
import com.example.service.SetmealService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    @SneakyThrows
    @Override
    public void saveWithDishes(SetmealDto setmealDto) {
        //在做这个之前,先查询表中is_deleted的集合中,有没有和他重名的,如果有则抛出异常
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper();
        wrapper.eq(Setmeal::getName,setmealDto.getName());
        Setmeal setmeal = super.getOne(wrapper);
        if(null != setmeal){
            throw new SQLIntegrityConstraintViolationException("Duplicate entry "+setmealDto.getName());
        }
        //首先保存套餐信息
        super.save(setmealDto);
        Long setmealId = setmealDto.getId();
        //然后为每一个套餐里面的餐品绑定一个套餐的id
        List<SetmealDish> collect = setmealDto.getSetmealDishes().stream().map((item) -> {
            item.setSetmealId(setmealId);
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(collect);
    }

    @Override
    public SetmealDto getWithDishesById(Long id) {
        SetmealDto setmealDto = new SetmealDto();
        //首先先得到这个setmeal
        Setmeal setmeal = super.getById(id);
        BeanUtils.copyProperties(setmeal,setmealDto);
        //然后拿着这个id去去找所有的菜品,记得是使用list()方法
        LambdaQueryWrapper<SetmealDish> wrapper = new LambdaQueryWrapper();
        wrapper.eq(SetmealDish::getSetmealId,id);
        List<SetmealDish> list = setmealDishService.list(wrapper);
        setmealDto.setSetmealDishes(list);
        return setmealDto;
    }

    @Override
    public void updateWithDishes(SetmealDto setmealDto) {
        //1.首先对套餐的菜品表进行更新,对数据进行删除,拿到dto的id,先去查表
        Long setmealId = setmealDto.getId();
        //对每个套餐内的菜品id进行修改,防止重复id
        List<SetmealDish> list = setmealDto.getSetmealDishes().stream().map((item) -> {
            item.setId(CustomIdentifierGenerator.getAssignID());
            item.setSetmealId(setmealId);
            return item;
        }).collect(Collectors.toList());
        setmealDto.setSetmealDishes(list);
        //设置is_deleted这个属性,进行软删除
        LambdaUpdateWrapper<SetmealDish> wrapper = new LambdaUpdateWrapper();
        wrapper.set(SetmealDish::getIsDeleted,1);//设置为1表示删除
        wrapper.eq(SetmealDish::getSetmealId,setmealId);
        //前一个为null的时候,就会只更新你设置字段
        setmealDishService.update(null,wrapper);
        //然后我们得到了新的菜品表之后,再重新执行插入的操作
        setmealDishService.saveBatch(setmealDto.getSetmealDishes());
        //套餐内菜品更新完毕后,更新菜品表
        LambdaUpdateWrapper<Setmeal> setmealLambdaUpdateWrapper = new LambdaUpdateWrapper();
        setmealLambdaUpdateWrapper.eq(Setmeal::getId,setmealId);
        super.update(setmealDto,setmealLambdaUpdateWrapper);
    }


    @Override
    @Transactional
    public void deleteByIds(String ids){
        Long[] dstIds = MyStringHandler.getAllIds(ids);
        for (Long dstId : dstIds) {
            log.info("目前正在删除的是:{}",dstId);
            LambdaUpdateWrapper<Setmeal> setmealWrapper = new LambdaUpdateWrapper();
            LambdaUpdateWrapper<SetmealDish> setmealDishWrapper = new LambdaUpdateWrapper();
            setmealWrapper.eq(Setmeal::getId,dstId);
            setmealWrapper.set(Setmeal::getIsDeleted,1);
            super.update(null,setmealWrapper);

            setmealDishWrapper.eq(SetmealDish::getDishId,dstId);
            setmealDishWrapper.set(SetmealDish::getIsDeleted,1);
            setmealDishService.update(null,setmealDishWrapper);
        }
    }

    @Override
    @Transactional
    public void changeStatusByIds(int status,String ids) {
        //scope.row.status == '0' ? '启售' : '停售'
        Long[] dstIds = MyStringHandler.getAllIds(ids);
        for (Long dstId : dstIds) {
            LambdaUpdateWrapper<Setmeal> dishWrapper = new LambdaUpdateWrapper();
            //删除菜品信息
            dishWrapper.eq(Setmeal::getId,dstId);
            dishWrapper.set(Setmeal::getStatus,status);
            this.update(null,dishWrapper);
        }
    }

    @Transactional
    @Override
    public Page pageByPageInfo(int page, int pageSize, String name) {
        //使用Mp的分页插件,如果有name的话就构造条件构造器
        //1.分页构造器
        Page pageInfo = new Page(page,pageSize);//查第page页,查pageSize条件
        //2.条件构造器,添加过滤条件
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper();//注意指定泛型
        lambdaQueryWrapper.like(!StringUtils.isBlank(name), Setmeal::getName,name);//where name = ,注意当name为空的时候不搞这个sql
        //3.保证查询数据的一致性,设置排序规则
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        //4.执行查询(分页构造器,条件构造器)
        Page data = super.page(pageInfo, lambdaQueryWrapper);
        //然后将套餐分类注入进去
        List<Setmeal> records = data.getRecords();
        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Category::getId, item.getCategoryId());
            Category category = categoryService.getOne(wrapper);
            BeanUtils.copyProperties(item, setmealDto);
            setmealDto.setCategoryName(category.getName());
            return setmealDto;
        }).collect(Collectors.toList());
        data.setRecords(list);
        return data;
    }

    @Override
    public List<Setmeal> getByCategoryId(Long categoryId,int status) {
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Setmeal::getCategoryId,categoryId);
        wrapper.eq(Setmeal::getStatus,status);
        wrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list = super.list(wrapper);
        return list;
    }
}
