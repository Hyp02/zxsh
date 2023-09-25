package com.hmdp.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 保存探店笔记
     * @param blog
     * @return
     */
    @Override
    public Result saveBlog(Blog blog) {
        // 校验
        Long shopId = blog.getShopId();
        String title = blog.getTitle();
        String content = blog.getContent();
        if (shopId == null) {
            return Result.fail("你还未关联店铺哦");
        }
        if (title == null) {
            return Result.fail("请填写标题");
        }
        if (content == null) {
            return Result.fail("请填写评价");
        }
        // 保存评价
        this.save(blog);
        return Result.ok(blog.getId());
    }

    /**
     * 在主页显示所有探店笔记
     * @param current
     * @return
     */

    @Override
    public Result queryHotBolg(Integer current) {
        // 根据用户查询
        Page<Blog> page = this.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            // 查询博主
            queryBlogUser(blog);
            // 查询blog是否被点赞
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    /**
     * 根据探店笔记 查看笔记详情
     * @param id
     * @return
     */
    @Override
    public Result queryBolgById(Long id) {
        // 根据id查出博客
        Blog blog = this.getById(id);
        if (blog == null) {
            Result.fail("探店笔记不存在");
        }
        // 设置博客中的发布信息的人
        this.queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    /**
     * 笔记是否已经点赞过
     * @param blog
     */
    private void isBlogLiked(Blog blog) {
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        // 判断用户是否已经点赞
        Double isMember = stringRedisTemplate.opsForZSet()
                .score(key, userId.toString());
        // 是否点赞赋值
        blog.setIsLike(isMember!=null);
    }

    /**
     * 点赞
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        // 判断用户是否已经点赞
        Double score = stringRedisTemplate.opsForZSet()
                .score(key, userId.toString());
        if (score == null)  {
            // 点赞 数据库中点赞数+1
            boolean isSuccess = this.update()
                    .setSql("liked=liked+1")
                    .eq("id", id).update();
            if (isSuccess) {
                // 并将点赞用户存储在点赞用户redis中,将时间戳作为source存入
                stringRedisTemplate.opsForZSet().add(key, userId.toString(),System.currentTimeMillis());
            }
        } else {
            // 取消点赞
            // 数据库中点赞数-1
            boolean isSuccess = this.update()
                    .setSql("liked=liked-1")
                    .eq("id", id).update();
            // 移除redis中点赞用户
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 显示点赞的用户top5
     * @param id
     * @return
     */
    @Override
    public Result queryBlogLikes(Long id) {
        // 获取前五名，使用range获取某个段内的用户
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(RedisConstants.BLOG_LIKED_KEY + id, 0, 5);
        if (top5 == null|| top5.isEmpty()) {
            // 如果没有人点赞，返回一个空集合
            return Result.ok(Collections.emptyList());
        }
        // 将前五名的用户id使用stream流转换为Long类型的list集合
        List<Long> ids = top5.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        // 将list集合中的id使用 , 拼接为字符串
        String idsStr = StrUtil.join(",", ids);
        // 将取出的用户转换为DTO
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids).last("ORDER BY FIELD(id,"+idsStr+")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    /**
     * 显示这个笔记中记录的用户，用来显示点赞人的头像等信息
     * @param blog
     */
    // 查询探店笔记中关联的用户信息，赋值给blog对象
    private void queryBlogUser(Blog blog) {
        // 根据博客中用户id查出发布信息的人
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
