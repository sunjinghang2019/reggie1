package com.example.filter;

import com.alibaba.fastjson.JSON;
import com.example.common.BaseContext;
import com.example.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经完成登录
 */
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
@Slf4j
//此注解有两个参数,第一个参数是该过滤器的名称,第二个是过滤器所需要拦截的路径,我们设置为所有请求路径都拦截判断一下
public class LoginCheckFilter implements Filter {//要实现一个过滤器,需要实现一个过滤器的接口
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();//路径匹配器,支持通配符
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        //1.获取本次请求的URI(请求路径)
        //URL是同一资源定位符,是网络上资源的地址,可以定义为引用地址的字符串,用于指示资源的位置以及用于访问它的协议
        //包括访问资源的协议 服务器的位置(IP或者是域名) 服务器上的端口号 在服务器目录结构中的位置 片段标识符
        //URI是表示逻辑或者物理资源的字符序列,与URL类似,也是一串字符,通过使用位置,名称或两者来表示
        //URL主要用来连接网页,网页组件或网页上的程序,借助访问方法
        //URI用于定义项目的标识
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse =(HttpServletResponse) servletResponse;
        log.info("拦截到请求:{}",((HttpServletRequest) servletRequest).getRequestURI());

        //2.判断本次请求是否需要处理,不需要处理则直接放行
        String requestURI = httpServletRequest.getRequestURI();
        //设置一个字符串集,表示不需要处理的路径
        String[] urls = new String[]{
                "/employee/login",//1.如果人家请求的路径就是来登录的,直接放行
                "/employee/logout",//2.退出则直接放行
                "/backend/**",  //关于页面的显示可以交给前端工程师来做,我们要做的是当用户未登录时,屏蔽请求数据的接口
                "/front/**",
                "/common/**",
                "/user/sendMsg",//移动端发送短信
                "/user/login"//移动端登录
        };

        if(check(urls,requestURI)){//不需要处理,直接放行
            log.info("不需要处理,直接放行");
            filterChain.doFilter(httpServletRequest,httpServletResponse);
            return;
        }

        //4-1.判断登录状态,如果已经登录,则直接放行(员工)
        if(httpServletRequest.getSession().getAttribute("employee") != null){//之前已经登录过的,直接放行
            log.info("用户已经登录");
            //取出id
            BaseContext.setCurrentUserId((Long)httpServletRequest.getSession().getAttribute("employee"));
            filterChain.doFilter(httpServletRequest,httpServletResponse);
            return;
        }

        //4-2.判断登录状态,如果已经登录,则直接放行(用户)
        if(httpServletRequest.getSession().getAttribute("user") != null){//之前已经登录过的,直接放行
            log.info("用户已经登录");
            //取出id
            BaseContext.setCurrentUserId((Long)httpServletRequest.getSession().getAttribute("user"));
            filterChain.doFilter(httpServletRequest,httpServletResponse);
            return;
        }

        //5.未登录则返回未登录结果,由于前端拦截的是我们的response对象,所以我们往response对象里面写result对象即可
        //res.data.code === 0 && res.data.msg === 'NOTLOGIN',这是前端的逻辑
        log.info("用户未登录");
        httpServletResponse.getWriter().write(JSON.toJSONString(Result.error("NOTLOGIN")));
    }

    /**
     * 请求本次是否需要放行
     * @param requestURI
     * @param urls
     * @return
     */
    public boolean check(String[] urls,String requestURI){
        for (String url : urls) {
            if(PATH_MATCHER.match(url,requestURI)){
                return true;
            }
        }
        return false;
    }


}
