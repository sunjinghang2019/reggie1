package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.domain.Employee;
import com.example.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")//截取请求
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录接口
     * @param httpServletRequest:当登录成功后,需要将部分响应得到(查询数据库)后的数据写回到session(在httpServletRequest)中,因此需要该参数进行操作
     * @param employee:
     * @return
     */
    @PostMapping("/login")//post请求,参数藏在请求体中,使用此注解取出数据
    public Result<Employee> login(HttpServletRequest httpServletRequest, @RequestBody Employee employee){
        //1.将页面提交的密码进行md5加密
        String passwordMd5 = DigestUtils.md5DigestAsHex(employee.getPassword().getBytes());
        //2.根据页面提交的用户名的username查询数据库,注意添加泛型
        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper();
        lambdaQueryWrapper.eq(Employee::getUsername,employee.getUsername());
        //调用service的getOne方法即可,这里数据库的设置是因为用户名是唯一的(UNIQUE)
        Employee user = employeeService.getOne(lambdaQueryWrapper);
        //3.如果没有查询到则返回登录失败的结果
        if(null == user){
            log.info("登录失败");
            return Result.error("登录失败");
        }

        //4.查到了接口,进行密码的校验
        if(!user.getPassword().equals(passwordMd5)){
            log.info("密码错误!登录失败");
            return Result.error("密码错误!登录失败");
        }

        //5.密码也比对成功,查看员工状态,注意Integer的缓存问题
        if(user.getStatus() == 0){//禁用
            log.info("账号已经禁用");
            return Result.error("账号已经禁用");
        }
        //6.登录成功
        httpServletRequest.getSession().setAttribute("employee",user.getId());
        System.out.println("登录成功!");
        return Result.success(user);
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest httpServletRequest){
        //1.清理session中保存的当前员工的id
        httpServletRequest.getSession().removeAttribute("employee");
        //2.返回结果
        return Result.success("退出成功!");
    }

    /**
     * 新增员工
     * @param employee
     * @return
     */
    @PostMapping
    public Result<String> save(@RequestBody Employee employee){
        log.info("新增员工,前台发过来的员工信息:{}",employee.toString());
        //新增员工时添加初始密码,注意要md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8)));
        //设置创建时间
        //employee.setCreateTime(LocalDateTime.now());
        //设置更新时间
        //employee.setUpdateTime(LocalDateTime.now());
        //设置创建人,拿到当前登录用户的session从而得到id
        //employee.setCreateUser((Long)request.getSession().getAttribute("employee"));
        //设置更新,拿到当前登录用户的session
        //employee.setUpdateUser((Long)request.getSession().getAttribute("employee"));

        employeeService.save(employee);//MP简洁方法
        return Result.success("新增员工成功");
    }

    @GetMapping("/page")
    public Result<Page> page(int page,int pageSize,String name){
        log.info("page={},pageSize={},name={}",page,pageSize,name);
        //使用Mp的分页插件,如果有name的话就构造条件构造器
        //1.分页构造器
        Page pageInfo = new Page(page,pageSize);//查第page页,查pageSize条件
        //2.条件构造器,添加过滤条件
        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper();//注意指定泛型
        lambdaQueryWrapper.like(!StringUtils.isBlank(name),Employee::getName,name);//where name = ,注意当name为空的时候不搞这个sql
        //3.保证查询数据的一致性,设置排序规则
        lambdaQueryWrapper.orderByDesc(Employee::getUpdateTime);
        //4.执行查询(分页构造器,条件构造器)
        Page data = employeeService.page(pageInfo, lambdaQueryWrapper);
        return Result.success(data);
    }

    /**
     * 根据id修改员工 信息,修改信息需要使用put
     * @param employee
     * @return
     */
    @PutMapping
    public Result<String> update(HttpServletRequest request,@RequestBody Employee employee){
        log.info("收到前台给的员工信息:{}",employee.toString());

        //收到信息后就开始update
        employee.setUpdateUser((Long) request.getSession().getAttribute("employee"));
        employee.setUpdateTime(LocalDateTime.now());

        employeeService.updateById(employee);
        return Result.success("修改成功");
    }

    @GetMapping("/{id}")
    public Result<Employee> getById(@PathVariable Long id){
        Employee employee = employeeService.getById(id);
        log.info("根据id查询员工信息{}",employee.toString());
        if(employee!=null){
            return Result.success(employee);
        }
        return Result.error("没有查询到对应的员工信息");
    }


}
