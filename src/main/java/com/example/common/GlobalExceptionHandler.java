package com.example.common;

import com.example.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class})//设置要拦截哪些controller
@ResponseBody//当异常处理完毕后,响应到前端
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 异常处理方法
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)//设置处理什么异常
    public Result<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error(ex.getMessage());//得到异常信息
        if(ex.getMessage().contains("Duplicate entry")){//是否包含特征值,做异常处理分发,这个是用来约束唯一索引的
            String[] split = ex.getMessage().split(" ");
            String msg = split[2]+"已存在";
            return Result.error(msg);
        }
        return Result.error("未知错误");
    }

    /**
     * 异常处理方法
     * @return
     */
    @ExceptionHandler(CustomException.class)//设置处理什么异常
    public Result<String> exceptionHandler(CustomException ex){
        log.error(ex.getMessage());//得到异常信息
        return Result.error(ex.getMessage());
    }


}
