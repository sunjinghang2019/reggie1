package com.example.common;

/**
 * 基于ThreadLocal的工具类:用于保存和获取当前登录用户的id
 */
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置值
     * @param id
     */
    public static void setCurrentUserId(Long id){
        threadLocal.set(id);
    }

    /**
     * 获取值
     * @return
     */
    public static Long getCurrentUserId(){
        return threadLocal.get();
    }

    public static void removeUserId(){
        threadLocal.remove();
    }

}
