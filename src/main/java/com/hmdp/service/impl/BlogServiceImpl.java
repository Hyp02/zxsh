package com.hmdp.service.impl;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.hmdp.utils.RedisConstants.FEED_KEY;

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
    @Resource
    private IFollowService followService;

    /**
     * 保存探店笔记
     * 推送笔记给粉丝
     *
     * @param blog
     * @return
     */
    @Override
    public Result saveBlog(Blog blog) {
        // 获取当前登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
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
        // 推送笔记Feed
        // 查询当前发布者的粉丝，所有关注我的人
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        for (Follow follow : follows) {
            // 获取粉丝id
            Long followId = follow.getUserId();
            String key = FEED_KEY + followId;
            // 推送到粉丝收件箱
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }
        //
        return Result.ok(blog.getId());
    }

    /**
     * 查看关注人的博客
     * 实现滚动分页
     * @param max
     * @param offset
     * @return
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 获取当前用户
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        // 获取当前用户的收件箱
        String key = FEED_KEY + userId;
        // 获取用户收件箱
        Set<ZSetOperations.TypedTuple<String>> inBox = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0,max, offset,3);
        if (inBox == null || inBox.isEmpty()) {
            return Result.ok();
        }
        // 实现滚动分页
        /**
         * 循环将收件箱中的元素取出来
         * 如何查找最小时间和最小时间 相同最小时间的个数有几个
         * 初始化最小时间为0
         * 将当前查出来的时间和最小时间作比较
         * 如果最小时间和查出来的时间相同 将数量加一 【第一次最小时间是0 不可能会有】
         * 如果不同，将查出来的时间当做最小的，进行下一次比较
         * 当出现不同的，说明又查出了更小的时间，将更小的时间和数量赋值和初始化
         * 这样就得到最小时间和最小时间对应的元素的个数
         */
        // 存放收件箱中博客id的集合
        List<Long> ids = new ArrayList<>();
        long minTime = 0; // 初始化最小时间
        int offsetCount = 1; // 初始化相同source元素个数
        for (ZSetOperations.TypedTuple<String> box : inBox) {
            // 给id集合中添加元素
            ids.add(Long.valueOf(box.getValue()));
            // 获取source
            long time = box.getScore().longValue();
            if (time == minTime) {
                // 如果当前时间就是最小时间。给offset+1
                offsetCount++;
            } else {
                // 如果当前时间不是最小时间
                // 将当前时间赋值给最小时间
                minTime = time;
                // 初始化offsetCount
                offsetCount = 1;
            }
        }
        // 根据博客id将博客id查出来
        String idsStr = StrUtil.join(",", ids);
        List<Blog> blogs = this.query()
                .in("id", ids)
                .last("ORDER BY FIELD(id," + idsStr + ")")
                .list();
        // 查看博客被点赞和关联的用户
        for (Blog blog : blogs) {
            // 查看发布博客的人【用来显示作者头像】
            this.queryBlogUser(blog);
            // 查看这个博客是否被你点赞
            this.isBlogLiked(blog);
        }
        // 封装返回对象
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setMinTime(minTime);
        scrollResult.setOffset(offsetCount);
        return Result.ok(scrollResult);
    }

    /**
     * 在主页显示所有探店笔记
     *
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
     *
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
     *
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
        blog.setIsLike(isMember != null);
    }

    /**
     * 点赞
     *
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        // 判断用户是否已经点赞
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet()
                .score(key, userId.toString());
        if (score == null) {
            // 点赞 数据库中点赞数+1
            boolean isSuccess = this.update()
                    .setSql("liked=liked+1")
                    .eq("id", id).update();
            if (isSuccess) {
                // 并将点赞用户存储在点赞用户redis中,将时间戳作为source存入
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
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
     *
     * @param id
     * @return
     */
    @Override
    public Result queryBlogLikes(Long id) {
        // 获取前五名，使用range获取某个段内的用户
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(RedisConstants.BLOG_LIKED_KEY + id, 0, 5);
        if (top5 == null || top5.isEmpty()) {
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
                .in("id", ids).last("ORDER BY FIELD(id," + idsStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }


    /**
     * 显示这个笔记中记录的用户，用来显示点赞人的头像等信息
     *
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
