package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Han
 * @data 2023/9/6
 * @apiNode
 */

public class LoginInterceptor implements HandlerInterceptor {
   /*
    //@Resource 注意这里不能使用注解，因为拦截器在webConfig中是new出来的，不是Spring生成的,所以要使用构造器注入
    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    */

    // 前置拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /*
        // 获取session中的信息
        //UserDTO userMap =(UserDTO) request.getSession().getAttribute("user");
        // 是否存在
        // 获取token
        String token = request.getHeader("authorization");
        if (StringUtils.isBlank(token)) {
            response.setStatus(401);
            return false;
        }
        String userKey = RedisConstants.LOGIN_USER_KEY+token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(userKey);
        if (userMap.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        // 在redis中存储的是Map类型，所以要在这将Map类型转换为User类型再存储在ThreadLocal
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 将用户存放在ThreadLocal中
        UserHolder.saveUser(userDTO);

        // 刷新有效时间
        stringRedisTemplate.expire(userKey, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return true;
    }
    */
        // 判断是否有登录的用户
        // 有登录的用户放行，没有拦截
        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }


    // 后置拦截
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除登录用户
        UserHolder.removeUser();
    }
}
