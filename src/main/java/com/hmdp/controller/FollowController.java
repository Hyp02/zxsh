package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.impl.FollowServiceImpl;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private FollowServiceImpl followService;

    /**
     * 查询是否关注
     * @param followUserId 博主id
     * @return
     */
    @GetMapping("/or/not/{followUserId}")
    public Result isFollow(@PathVariable("followUserId") Long followUserId) {
        return followService.isFollow(followUserId);
    }

    /**
     * 关注博主
     * @param followUserId
     * @param is
     * @return
     */
    @PutMapping("/{followUserId}/{isFollow}")
    public Result follow(@PathVariable("followUserId") Long followUserId,
                           @PathVariable("isFollow") boolean is) {
        return followService.follow(followUserId,is);
    }

    /**
     * 共同关注
     * @return
     */
    @GetMapping("/common/{id}")
    public Result commonUser(@PathVariable("id")Long userId){
        return followService.commonFollow(userId);

    }

}
