package com.example;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

//lombok提供的日志工具
@Slf4j
@ServletComponentScan//此注解用于激活spring去扫描拦截器
@EnableTransactionManagement//激活事务
@EnableCaching//开启Spring Cache缓存
@SpringBootApplication
public class ReggieApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReggieApplication.class,args);//运行此类
        log.info("项目构建完成:2022-8-21");//使用log在控制台中输出日志
    }

}
