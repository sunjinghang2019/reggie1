package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BaseContext;
import com.example.common.Result;
import com.example.domain.ShoppingCart;
import com.example.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 获取购物车内的菜品集合,拿着用户id来查
     * @return
     */
    @GetMapping("/list")
    public Result<List<ShoppingCart>> list(){
        log.info("查看购物车");
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper();
        wrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentUserId());
        wrapper.orderByAsc(ShoppingCart::getCreateTime);
        return Result.success(shoppingCartService.list(wrapper));
    }

    /**
     * 添加购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public Result<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("得到的购物车数据是:{}",shoppingCart);
        //1.设置当前购物车数据是属于哪个用户的[设置用户id]
        shoppingCart.setUserId(BaseContext.getCurrentUserId());//获取当前用户的id
        //2.添加菜品到购物车
        //如果同一个菜品要了多份,只需要修改菜品的份数即可,而不需要插入多条数据
        //如果在购物车中了,那么就直接将数量+1
        //否则不在购物车中,那么就新建记录
        //2.1 首先判断是菜品还是套餐
        //如果获取到的套餐id不是空,那么认为它就是套餐
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper();
        wrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentUserId());
        if(shoppingCart.getSetmealId()!=null){
            wrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }else{//否则就是菜品
            wrapper.eq(ShoppingCart::getDishId,shoppingCart.getDishId());
        }
        //然后进行查询,如果发现有这条数据了,那么就直接++
        ShoppingCart one = shoppingCartService.getOne(wrapper);
        if(one != null){
            one.setNumber(one.getNumber()+1);
            shoppingCartService.updateById(one);
        }else{//否则的话就是发现没有这条数据,那么就直接将数据保存到数据库即可
            shoppingCart.setNumber(1);
            shoppingCartService.save(shoppingCart);
            one =shoppingCart;
        }
        one.setCreateTime(LocalDateTime.now());
        return Result.success(one);
    }


    @PostMapping("/sub")
    public Result<String> sub(@RequestBody ShoppingCart shoppingCart){
        //分类
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentUserId());
        if(shoppingCart.getDishId() != null){
            wrapper.eq(ShoppingCart::getDishId,shoppingCart.getDishId());
        }else if(shoppingCart.getSetmealId() != null){
            wrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }
        ShoppingCart cartServiceOne = shoppingCartService.getOne(wrapper);
        if(cartServiceOne.getNumber() >1 ){
            cartServiceOne.setNumber(cartServiceOne.getNumber()-1);
            //然后更新数据
            shoppingCartService.updateById(cartServiceOne);
        }else if(cartServiceOne.getNumber() == 1){
            //直接删除该条数据即可
            shoppingCartService.remove(wrapper);
        }
        return Result.success("操作成功");
    }

    @DeleteMapping("/clean")
    public Result<String> clean(){
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper();
        wrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentUserId());
        shoppingCartService.remove(wrapper);
        return Result.success("清空购物车成功");
    }




}
