package com.example.common;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;

/**
 * 自定义id生成器
 */
public class CustomIdentifierGenerator {
    public static Long getAssignID(){
        return new DefaultIdentifierGenerator().nextId(null);
    }



}
