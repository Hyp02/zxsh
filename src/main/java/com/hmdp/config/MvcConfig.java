package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @author Han
 * @data 2023/9/6
 * @apiNode
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 注册拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 配置不需要拦截的接口
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/shop/**" ,
                        "/shop-type/**",
                        "/voucher/**",
                        "/upload/**",
                        "blog/**",
                        "/user/code",
                        "/user/login"
                ).order(1);
        // 刷新用户登录态的拦截器拦截所有请求
        registry.addInterceptor(new RefreshInterceptor(stringRedisTemplate))
                .addPathPatterns("/**").order(0);

    }
}
