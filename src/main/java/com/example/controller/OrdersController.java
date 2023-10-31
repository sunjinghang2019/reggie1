package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.domain.Orders;
import com.example.dto.OrdersDto;
import com.example.service.OrdersService;
import com.sun.org.apache.xpath.internal.operations.Or;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    /**
     *
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody Orders orders){
        log.info("用户提交的订单参数:{}:",orders);
        //用户随时可以从session中获取userid或者是从数据库购物车表中获得下单的菜品
        ordersService.submit(orders);
        return Result.success("订单提交成功");
    }

    @GetMapping("/userPage")
    public Result<Page> userPage(int page, int pageSize){
        Page data = ordersService.userPage(page,pageSize);
        return Result.success(data);
    }

    @GetMapping("/page")
    public Result<Page> page(int page, int pageSize, String number, String beginTime, String endTime){
        Page pageInfo = ordersService.page(page, pageSize, number, beginTime, endTime);
        return Result.success(pageInfo);
    }

    @PutMapping
    public Result<String> changeStatus(@RequestBody Orders orders){
        ordersService.updateById(orders);
        return Result.success("更改派送状态成功!");
    }

    @PostMapping("/again")
    public Result<String> again(@RequestBody Orders orders){
        //将当前对应的订单一模一样复制一份到购物车中
        log.info("得到的上次订单信息为:{}",orders);
        return Result.success("返回成功");
    }


}
