package com.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.service.impl.BlogServiceImpl;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;
    @Resource
    private BlogServiceImpl blogService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;



    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // TODO 发送短信验证码并保存验证码
        Result result = userService.sendCode(phone, session);
        return result;
    }

    /**
     * 登录功能
     *
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        // TODO 实现登录功能
        Result result = userService.userLogin(loginForm, session);
        return result;
    }

    /**
     * 登出功能
     *
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        // TODO 实现登出功能
        String token = request.getHeader("authorization");
        if ("".equals(token) || token == null ) {
            return Result.fail("退出失败");
        }
        UserHolder.removeUser();
        String userToken = LOGIN_USER_KEY + token;
        stringRedisTemplate.delete(userToken);
        return Result.ok("退出成功，请刷新页面");
    }

    @GetMapping("/me")
    public Result me() {
        // 获取用户信息返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);

    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    /**
     * 显示博主资料
     * @param userId
     * @return
     */
    @GetMapping("/{id}")
    public Result blog(@PathVariable("id") Long userId){
        // 根据返回的博主的id查询
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        // 返回安全信息
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);

    }
    /**
     * 用户签到
     */
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }
    /**
     * 统计用户连续签到
     */
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}
