package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.domain.Orders;
import com.example.dto.OrdersDto;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface OrdersService extends IService<Orders> {
    void submit(Orders orders);
    Page userPage(int page,int pageSize);
    Page page(int page, int pageSize, String number, String beginTime, String endTime);
    void again(Orders orders);
}
