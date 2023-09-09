package com.hmdp.service.impl;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询店铺列表，使用Redis进行缓存
     * @return
     */
    @Override
    public Result queryShopList() {
        // 查询redis
        String listJson = stringRedisTemplate.opsForValue().get(RedisConstants.CATCH_SHOP_LIST);
        // 命中返回
        if (listJson != null) {
            // 将得到的json字符串转换为list
            List list = JSONUtil.parseArray(listJson);
            return Result.ok(list);
        }
        // 未命中 查询数据库并且排序
        List<ShopType> shopSort = query().orderByAsc("sort").list();
        // 数据库中不存在，返回错误信息
        if (shopSort == null) {
            return Result.fail("未查找到店铺类型");
        }
        // 数据存在
        // 将数据库中数据转化为json
        String shopJson = JSONUtil.toJsonStr(shopSort);
        //写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CATCH_SHOP_LIST, shopJson);
        // 返回数据
        return Result.ok(shopSort);
    }
}
