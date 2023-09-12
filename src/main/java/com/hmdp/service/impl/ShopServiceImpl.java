package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.HotShop;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    // 注入操作Redis的对象
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    /**
     * 查询店铺信息并使用redis缓存
     * 解决缓存穿透
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 互斥锁解决缓存击穿
        //Shop shop = this.queryWithMutex(id);
        // 判断店铺是否是热点店铺
        Shop shop = null;
        if (Arrays.asList(HotShop.hotShopId).contains(id)) {
            // 指定逻辑过期解决高并发问题的缓存击穿
            shop = cacheClient.queryWithLogicExpiredTime(RedisConstants.CACHE_SHOP_KEY,id, Shop.class, this::getById,
                    20L,TimeUnit.SECONDS);
            if (shop == null){
                Result.fail("店铺不见了");
            }
            return Result.ok(shop);
        }
        // 如果不是热点店铺，并发不高
        // 空缓存解决缓存穿透
        //shop = this.queryWithPassThorough(id);
        shop = cacheClient.queryWithPassThorough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,
                RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES, this::getById);
        if (shop == null) {
            Result.fail("店铺不见了");
        }
        return Result.ok(shop);
    }

    /**
     * 更新店铺
     * 确保缓存数据一致
     *
     * @param shop 店铺对象
     * @return
     */
    @Override
    @Transactional //添加事务
    public Result updateShopById(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("餐厅不存在");
        }
        // 修改数据库
        boolean b = updateById(shop);
        if (!b) {
            return Result.fail("更新店铺信息失败");
        }
        // 删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok();
    }

}
