package com.example.common;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用返回结果,服务端响应的数据最终都会封装成此对象
 * @param <T>
 */
@Data
public class Result <T> implements Serializable {
    private Integer code;//编码:1成功,0和其他数字为失败

    private String msg;//错误信息

    private T data;//回传数据,不定泛型

    private Map map = new HashMap();//动态数据

    public static <T> Result<T> success(T object){//通用响应结果方法
        Result<T> result = new Result<T>();
        result.data = object;
        result.code = 1;
        return result;
    }

    public static <T> Result<T> error(String msg){//通用响应结果方法
        Result<T> result = new Result<T>();
        result.msg = msg;
        result.code = 0;
        return result;
    }

    public Result<T> add(String key,Object value){//操作动态对象
        this.map.put(key,value);
        return this;
    }

}
