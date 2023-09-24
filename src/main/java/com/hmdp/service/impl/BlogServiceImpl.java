package com.hmdp.service.impl;

import java.time.LocalDateTime;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

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
        this.save(blog);
        return Result.ok(blog.getId());
    }
}
