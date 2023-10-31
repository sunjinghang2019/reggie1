package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.api.R;
import com.example.common.CustomIdentifierGenerator;
import com.example.common.Result;
import com.example.domain.User;
import com.example.service.UserService;
import com.example.utils.NameUtil;
import com.example.utils.SMSUtils;
import com.example.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public Result<String> sendMsg(@RequestBody User user, HttpSession session){
        //获取手机号
        String phone = user.getPhone();
        if(!StringUtils.isBlank(phone)){
            //生成随机的4位验证码,TODO:此处使用redis进行缓存优化,将session中缓存的验证码数据提交到redis
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("生成的验证码是:{}",code);
            //调用阿里云提供的短信服务API完成发送短信
            //SMSUtils.sendMessage("阿里云短信测试","SMS_154950909",phone,code);
            //将验证码保存到redis中,并且设置有效期为5分钟
            redisTemplate.opsForValue().set(phone,code, 5,TimeUnit.MINUTES);

            return Result.success("短信发送成功");
        }
        return Result.error("短信发送失败");
    }

    /**
     * 这里可以用map的原因是,封装成的json数据本身就是一条条的key-val,因此用map可以存储
     * @param user
     * @param session
     * @return
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody Map user,HttpSession session){
        log.info("获取到的用户信息是:{}",user);
        //redis中获取缓存的验证码
        //String codeInSession = (String)session.getAttribute((String) user.get("phone"));
        String code = (String) user.get("code");//用户输入的验证码
        String phone = (String) user.get("phone");//用户输入的手机号
        String codeInRedis = (String) redisTemplate.opsForValue().get(phone);
        if( codeInRedis!= null && code.equals(codeInRedis)){
            //判断当前手机号的用户是否是新用户,如果是新用户就自动完成注册
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper();
            wrapper.eq(User::getPhone,phone);
            User oneUser = userService.getOne(wrapper);
            Long userId ;
            if(oneUser == null){
                oneUser= new User();
                oneUser.setPhone(phone);
                oneUser.setStatus(1);
                userId = CustomIdentifierGenerator.getAssignID();
                oneUser.setId(userId);
                oneUser.setName(NameUtil.getStringRandom(8));//生成8位的名字
                userService.save(oneUser);
            }
            userId = oneUser.getId();
            session.setAttribute("user",userId);
            //如果用户登录成功,那么就删除Redis中缓存的验证码
            redisTemplate.delete(phone);
            return Result.success(oneUser);
        }
        return Result.error("登录失败");
    }

    @PostMapping("/loginout")
    public Result<String> logout(HttpServletRequest httpServletRequest){
        httpServletRequest.getSession().removeAttribute("user");
        return Result.success("退出成功!");
    }



}
