package com.example.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 元数据对象处理器
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    /**
     * 插入操作时自动填充
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {//在插入时执行
        log.info("公共字段自动填充[insert],原始对象:{}",metaObject.toString());
        metaObject.setValue("createTime", LocalDateTime.now());//写属性名=>值
        metaObject.setValue("updateTime", LocalDateTime.now());
        //此处需要获取到request=>session=>Attribute中的id属性,暂时获得不了
        metaObject.setValue("createUser",BaseContext.getCurrentUserId());
        metaObject.setValue("updateUser",BaseContext.getCurrentUserId());
    }

    /**
     * 更新操作时自动填充
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {//在更新时执行
        log.info("公共字段自动填充[update],原始对象:{}",metaObject.toString());
        metaObject.setValue("updateTime", LocalDateTime.now());
        metaObject.setValue("updateUser",BaseContext.getCurrentUserId());
    }
}
