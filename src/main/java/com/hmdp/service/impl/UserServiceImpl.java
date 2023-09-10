package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.DEFAULT_USER_NICK;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号判断是否符合
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 不符合
            return Result.fail("手机号格式错误！！");
        }
        // 符合(生成6位数字验证码并保存在session中)
        String code = RandomUtil.randomNumbers(6);
        // 保存验证码
        // session.setAttribute("code", code);
        // 修改验证码保存到Redis中并设置验证码的有效时间[k-手机号，v-验证码]
        stringRedisTemplate.opsForValue()
                .set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 发送验证码【因为发送验证码需要调用第三发Api,这里使用日志记录】
        log.debug("发送验证码成功 验证码为：{}", code);
        // 返回成功信息
        return Result.ok();
    }

    @Override
    public Result userLogin(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        // 校验
        if (StringUtils.isAnyBlank(phone)) {
            Result.fail("手机号不能为空");
        }
        if (StringUtils.isAnyBlank(code)) {
            Result.fail("验证码不能为空");
        }
        if (RegexUtils.isPhoneInvalid(phone)) {
            Result.fail("手机号格式不正确");
        }
        // 检验验证码是否正确
        // Object cacheCode = session.getAttribute("code");
        /** 修改: 从Redis中取出验证码 **/
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (StringUtils.isBlank(cacheCode)) {
            Result.fail("请先获取验证码");
        }
        if (!code.equals(cacheCode)) {
            Result.fail("验证码不正确");
        }
        // 查询用户是否存在
        User user = query().eq("phone", phone).one();
        if (user == null) {
            // 不存在自动注册
            user = createUserWithPhone(phone);
        }

        // 脱敏
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // Map类型
        Map<String, String> userMap = new HashMap<>();
        userMap.put("id", String.valueOf(userDTO.getId()));
        userMap.put("nickName", userDTO.getNickName());
        userMap.put("icon", userDTO.getIcon());

        // 将脱敏用户保存 [记录登录用户]
        // session.setAttribute("user", userDTO);
        /**
         * 修改：将用户存储在Redis中,使用hash类型存储，
         * 使用UUID作为k,原因是如果使用了手机号,Redis就会将手机号作为token的一部分发送给前端，从而产生安全问题
         * 将用户对象转换为Map类型，使用putAll()存放在Redis中
         */
        // 保存登录用户到redis中
        String token = UUID.randomUUID().toString(true);
        String userToken = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(userToken, userMap);
        /**
         * 这里设置的是无论用户怎么操作，一到30分钟就会过期
         * 但是我们需要的是当用户没有任何操作时，超过30分钟，才过期
         * 如果用户进行操作，这个过期时间就会一直刷新
         * 但是我们怎么知道用户到底在不在操作呢？
         *      答：当用户操作时就会触发拦截器，所以判断用户是否操作就是判断拦截器是否触发
         */
        // 设置有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 返回token
        return Result.ok(token);
    }

    // 用户脱敏【未使用的方法】
    private User safetyUser(User user) {
        User safetyUser = new User();
        safetyUser.setNickName(user.getNickName());
        safetyUser.setIcon(user.getIcon());
        safetyUser.setId(user.getId());
        return safetyUser;
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(DEFAULT_USER_NICK + RandomUtil.randomNumbers(4));
        // 保存用户
        boolean save = save(user);
        if (!save) {
            Result.fail("用户注册失败，请重试");
        }
        return user;
    }
}
