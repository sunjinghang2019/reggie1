package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.BaseContext;
import com.example.common.JacksonObjectMapper;
import com.example.domain.*;
import com.example.dto.OrdersDto;
import com.example.dto.SetmealDto;
import com.example.exception.CustomException;
import com.example.mapper.OrdersMapper;
import com.example.service.*;
import com.example.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
@Slf4j
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private UserService userService;
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private OrderDetailService orderDetailService;

    /**
     *
     * @param orders
     * @return
     */
    @Override
    @Transactional
    public void submit(Orders orders) {
        //1.首先拿到当前用户的id
        Long currentUserId = BaseContext.getCurrentUserId();
        //2.获取当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId,currentUserId);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(shoppingCartLambdaQueryWrapper);
        AtomicInteger amount = new AtomicInteger(0);//对于金额这种敏感的数据,需要使用线程安全类
        long orderId = IdWorker.getId();//生成订单号,这个不同于数据库的主键
        //重要数据的后端校验
        if(shoppingCartList == null){
            throw new CustomException("购物车为空,不能下单");
        }
        if(shoppingCartList.size() == 0){
            throw new CustomException("购物车为空,不能下单");
        }
        //计算总金额,批量保存订单明细,把订单明细数据封装出来
        List<OrderDetail> orderDetails = shoppingCartList.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());

            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());//计算金额的式子
            return orderDetail;
        }).collect(Collectors.toList());

        //查询用户数据,查询地址数据
        User user = userService.getById(currentUserId);

        //查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if(addressBook == null){
            throw new CustomException("地址信息有误,不能下单");
        }

        //根据以上信息,封装属性
        //3.向订单表插入数据,一次下单对应一条数据
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));//订单总的金额,后端需要重新校验计算一次,防止前端篡改
        orders.setUserId(currentUserId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        //向订单表插入数据，一条数据
        this.save(orders);
        //4.向订单明细表插入数据,有可能有多条数据
        orderDetailService.saveBatch(orderDetails);
        //提交完成之后,将购物车的数据全部删除
        LambdaQueryWrapper<ShoppingCart> shoppingCartWrapper = new LambdaQueryWrapper();
        shoppingCartWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentUserId());
        shoppingCartService.remove(shoppingCartWrapper);
    }

    @Override
    public Page userPage(int page,int pageSize) {
        //使用Mp的分页插件,如果有name的话就构造条件构造器
        //1.分页构造器
        Page pageInfo = new Page(page,pageSize);//查第page页,查pageSize条件
        //2.条件构造器,添加过滤条件
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper();//注意指定泛型
        //查询当前用户的所有订单信息
        lambdaQueryWrapper.eq(Orders::getUserId,BaseContext.getCurrentUserId());
        //3.保证查询数据的一致性,设置排序规则
        lambdaQueryWrapper.orderByDesc(Orders::getOrderTime);
        //4.执行查询(分页构造器,条件构造器)
        Page data = super.page(pageInfo, lambdaQueryWrapper);
        //然后将菜品信息注入进去
        List<Orders> records = data.getRecords();
        List<OrdersDto> list = records.stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderDetail::getOrderId, item.getId());
            List<OrderDetail> orderDetailList = orderDetailService.list(wrapper);
            BeanUtils.copyProperties(item, ordersDto);
            ordersDto.setOrderDetails(orderDetailList);
            return ordersDto;
        }).collect(Collectors.toList());
        data.setRecords(list);
        return data;
    }


    @Override
    public Page page(int page, int pageSize, String number, String beginTime, String endTime) {
        //page和size这两个参数是一定有的,所以我们先处理这个
        //使用Mp的分页插件,如果有name的话就构造条件构造器
        //1.分页构造器
        log.info("前端传过来的数据是:page:{},pageSize:{},number:{},beginTime:{},endTime:{}",page,pageSize,number,beginTime,endTime);
        Page pageInfo = new Page(page,pageSize);//查第page页,查pageSize条件
        //2.条件构造器,添加过滤条件
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper();//注意指定泛型
        //当传来订单号时,查询相关信息
        lambdaQueryWrapper.eq(!StringUtils.isBlank(number),Orders::getNumber,number);
        //3.保证查询数据的一致性,设置排序规则
        lambdaQueryWrapper.orderByDesc(Orders::getOrderTime);
        //4.执行查询(分页构造器,条件构造器)
        Page<OrdersDto> data = super.page(pageInfo, lambdaQueryWrapper);
        //假设说传来了结束时间,就需要拿到这些信息,来进行查询,进行过滤操作,这里使用流式计算来过滤
        //过滤之后,再往数据里面填用户名
        if(beginTime!= null && endTime!=null){
            LocalDateTime beginTimeLocal = TimeUtils.parseStringToTime(beginTime);
            LocalDateTime endTimeLocal = TimeUtils.parseStringToTime(endTime);
            List<OrdersDto> collect = data.getRecords().stream().
                    filter(item -> TimeUtils.isInRange(beginTimeLocal, item.getOrderTime(), endTimeLocal))
                    .collect(Collectors.toList());
            data.setRecords(collect);
        }
        return data;
    }

    @Override
    public void again(Orders orders) {

    }
}
