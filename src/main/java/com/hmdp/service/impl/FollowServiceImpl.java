package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.COMMON_FOLLOW_USER;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result isFollow(Long followUserId) {
        // 获取当前登录用户id
        Long userId = UserHolder.getUser().getId();
        // 查看关注表中这个用户有没有关注博主
        // select COUNT(*) form follow
        //          where user_id = userId and follow_user_id = followUserId
        Integer count = this.query().eq("user_id", userId)
                .eq("follow_user_id", followUserId)
                .count();
        return Result.ok(count > 0);
    }

    @Override
    public Result follow(Long followUserId, boolean is) {
        // 获取当前用户id
        Long userId = UserHolder.getUser().getId();
        // 判断是否关注
        if (is) {
            // 新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSave = this.save(follow);
            if (isSave) {
                // 新增数据的同时将数据写入redis中，用来做交集共同关注
                stringRedisTemplate.opsForSet().add(COMMON_FOLLOW_USER + userId, followUserId.toString());
            }
        } else {
            // 移除数据 从follow表移除userid关注的followUserId
            // delete from follow where userid = ? and followId = ?
            QueryWrapper<Follow> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            wrapper.eq("follow_user_id", followUserId);
            boolean remove = this.remove(wrapper);
            if (remove) {
                // 取关删除
                stringRedisTemplate.opsForSet().remove(COMMON_FOLLOW_USER + userId, followUserId.toString());
            }
        }

        return Result.ok();
    }

    /**
     * 查看共同关注
     * @param userId
     * @return
     */
    @Override
    public Result commonFollow(Long userId) {
        // 获取当前登录用户id
        Long loginId = UserHolder.getUser().getId();
        String key1 = COMMON_FOLLOW_USER + userId;
        String key2 = COMMON_FOLLOW_USER + loginId;
        // 查看交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok();
        }
        // 将用户id全部转换为Long型
        List<Long> ids = intersect.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        // 将查出来的用户装换为DTO类
        List<UserDTO> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }
}
