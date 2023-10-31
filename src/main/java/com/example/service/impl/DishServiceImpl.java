package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.CustomIdentifierGenerator;
import com.example.common.MyStringHandler;
import com.example.domain.Category;
import com.example.domain.Dish;
import com.example.domain.DishFlavor;
import com.example.dto.DishDto;
import com.example.mapper.DishMapper;
import com.example.service.DishFlavorService;
import com.example.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 新增菜品,同时插入菜品对应的口味数据,需要同时操作两张表:dish、dish_flavor
     * @param dishDto
     */
    @Override
    @Transactional//由于涉及到多张表的操作,因此这里需要开启事务控制,这是为了保证数据库表的一致性
    public void saveWithFlavor(DishDto dishDto){
        //保存菜品的基本信息到菜品表dish
        this.save(dishDto);
        //保存菜品口味数据到菜品口味表dish_flavor
        Long dishId = dishDto.getId();
        //获取id
        List<DishFlavor> flavors = dishDto.getFlavors();
        //获取口味列表
        List<DishFlavor> collect = putIdIn(flavors,dishId);
        //流式计算存取数据
        dishFlavorService.saveBatch(collect);
        String key = "dish_"+dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);
    }

    /**
     * 多表联查,将DTO结果给回页面渲染
     * @param id
     * @return
     */
    @Override
    public DishDto getIdWithFlavour(Long id){
        //查询菜品基本信息,从dish表查询
        Dish dish = this.getById(id);
        //查询当前菜品对应的口味信息,从dish_flavour表查询,由于口味可能出现多个,因此要进行条件查询
        LambdaQueryWrapper<DishFlavor> wrapper = new LambdaQueryWrapper();
        wrapper.eq(DishFlavor::getDishId,dish.getId());
        List<DishFlavor> list = dishFlavorService.list(wrapper);

        DishDto dishDto = new DishDto();
        //进行对象拷贝
        BeanUtils.copyProperties(dish,dishDto);//src,dst
        dishDto.setFlavors(list);
        return dishDto;
    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表
        log.info("正在执行dish表的操作");
        this.updateById(dishDto);

        //更新口味表
        //1.清理当前菜品对应口味数据,重置
        //delete from dish_flavor where dish_id = ???
        log.info("正在执行dish_flavor表的操作");
        LambdaQueryWrapper<DishFlavor> wrapper = new LambdaQueryWrapper();
        wrapper.eq(DishFlavor::getDishId,dishDto.getId());
        //正常删除分类LambdaQueryWrapper
        LambdaUpdateWrapper<DishFlavor> dishFlavorWrapper = new LambdaUpdateWrapper();
        dishFlavorWrapper.set(DishFlavor::getIsDeleted,1);
        dishFlavorWrapper.eq(DishFlavor::getDishId,dishDto.getId());
        //第一个参数要设置为null,这样就只会更新你set的字段
        dishFlavorService.update(null,dishFlavorWrapper);
        //2.再来针对当前提交过来的口味数据,执行插入操作
        List<DishFlavor> flavors = dishDto.getFlavors();
        dishFlavorService.saveBatch(putIdIn(flavors,dishDto.getId()));
        //清理所有菜品的缓存数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);
        //精确清理缓存数据

        //只是清理某个分类下面的菜品缓存数据
        String key = "dish_"+dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);
    }

    @Override
    @Transactional
    public void deleteByIds(String ids){
        Long[] dstIds = MyStringHandler.getAllIds(ids);
        //删除菜品信息和口味信息表的信息
        for (Long dstId : dstIds) {
            log.info("目前正在删除的是:{}",dstId);
            LambdaUpdateWrapper<Dish> dishWrapper = new LambdaUpdateWrapper();
            LambdaUpdateWrapper<DishFlavor> dishFlavorWrapper = new LambdaUpdateWrapper();
            LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
            //清理缓存数据
            dishQueryWrapper.eq(Dish::getId,dstId);
            Dish dish = this.getOne(dishQueryWrapper);
            String key = "dish_"+dish.getCategoryId()+"_1";
            redisTemplate.delete(key);

            //删除菜品信息
            dishWrapper.eq(Dish::getId,dstId);
            dishWrapper.set(Dish::getIsDeleted,1);
            this.update(null,dishWrapper);

            //删除口味信息
            dishFlavorWrapper.eq(DishFlavor::getDishId,dstId);
            dishFlavorWrapper.set(DishFlavor::getIsDeleted,1);
            dishFlavorService.update(null,dishFlavorWrapper);

        }
    }

    @Override
    @Transactional
    public void changeStatusByIds(int status,String ids) {
        //scope.row.status == '0' ? '启售' : '停售'
        Long[] dstIds = MyStringHandler.getAllIds(ids);
        for (Long dstId : dstIds) {
            //更新菜品的售卖状态
            LambdaUpdateWrapper<Dish> dishWrapper = new LambdaUpdateWrapper();
            dishWrapper.eq(Dish::getId,dstId);
            dishWrapper.set(Dish::getStatus,status);
            this.update(null,dishWrapper);

            //清理缓存数据
            LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishLambdaQueryWrapper.eq(Dish::getId,dstId);
            Dish dish = this.getOne(dishLambdaQueryWrapper);
            String key = "dish_"+dish.getCategoryId()+"_1";
            redisTemplate.delete(key);
        }
    }

    @Override
    public List<DishDto> getDishDtoListByCategory(Dish dish) {
        List<DishDto> dishDtos  = null;
        //1.先从redis中缓存数据
        //动态构造key
        String key = "dish_"+dish.getCategoryId()+"_"+dish.getStatus();
        //如果存在,直接返回,无需返回数据库
        dishDtos = (List<DishDto>) redisTemplate.opsForValue().get(key);
        //如果不存在,需要查询数据库,将查询带的菜品数据缓存到redis
        if(dishDtos!=null){
            log.info("使用的是redis里面的数据");
            return dishDtos;
        }

        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(dish!=null,Dish::getCategoryId,dish.getCategoryId());
        lambdaQueryWrapper.eq(dish!=null,Dish::getStatus,1);//只查起售的
        lambdaQueryWrapper.orderByDesc(Dish::getSort);
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        List<Dish> list = super.list(lambdaQueryWrapper);

        List<DishDto> dishDtoes = list.stream().map((item) -> {
            //1.复制基本属性
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            //2.拿着dishId去查口味表
            LambdaQueryWrapper<DishFlavor> dishFlavorWrapper = new LambdaQueryWrapper<>();
            dishFlavorWrapper.eq(DishFlavor::getDishId, item.getId());
            dishDto.setFlavors(dishFlavorService.list(dishFlavorWrapper));
            return dishDto;
        }).collect(Collectors.toList());
        //首次查询数据库,将查询到的缓存到redis
        log.info("使用的是mysql里面的数据");
        redisTemplate.opsForValue().set(key,dishDtoes,60, TimeUnit.MINUTES);//缓存60分钟
        return dishDtoes;
    }

    public List<DishFlavor> putIdIn(List<DishFlavor> flavors, Long dishId){
        return flavors.stream().map((item) -> {
            item.setId(CustomIdentifierGenerator.getAssignID());
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());
    }





}
