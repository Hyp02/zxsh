package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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

    /**
     * 查询店铺信息并使用redis缓存
     * 解决缓存穿透
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        //Shop shop = queryWithPassThorough(id);
        Shop shop = queryWithMutex(id);
        if (shop == null){
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

    /**
     * 解决缓存穿透查询店铺函数
     *
     * @param id
     * @return
     */
    private Shop queryWithMutex(Long id) {
        // 查询redis【这里的数据类型可选择Hash和String(这次选择String)】
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 命中 返回
        if (StringUtils.isNotBlank(shopJson)) {
            // 转换为对象后返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 如果是写入的空缓存
        if ("".equals(shopJson)) {
            // 结束
            return null;
        }
        // 未命中 获取互斥锁,
        String lockKey = "lock:shop" + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 是否获取到了锁
            if (!isLock) {
                // 未获得 休眠
                Thread.sleep(50);
                // 递归 重新查询缓存
                return queryWithMutex(id);
            }
            // 获得了锁 重建缓存
            shop = getById(id);
            Thread.sleep(200);
            if (shop == null) {
                // 缓存穿透解决 写入空值
                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "",
                        RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            String toJsonShop = JSONUtil.toJsonStr(shop);
            // 数据库中存在 先保存在redis中再返回
            // 设置店铺缓存失效时间
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, toJsonShop,
                    RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
            // 释放互斥锁
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 释放锁
            delLock(lockKey);
        }
        // 返回信息
        return shop;
    }

    /**
     * 互斥锁
     *
     * @param id
     * @return
     */
    private Shop queryWithPassThorough(Long id) {
        // 查询redis【这里的数据类型可选择Hash和String(这次选择String)】
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 命中 返回
        if (StringUtils.isNotBlank(shopJson)) {
            // 转换为对象后返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 如果是写入的空缓存
        if (shopJson == "") {
            // 结束
            return null;
        }
        // 未命中 在数据库中查
        // 数据库中不存在 返回错误信息【getById是接口中的方法】
        Shop shop = getById(id);
        if (shop == null) {
            // 缓存穿透解决 写入空值
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "",
                    RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        String toJsonShop = JSONUtil.toJsonStr(shop);
        // 数据库中存在 先保存在redis中再返回
        // 设置店铺缓存失效时间
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, toJsonShop,
                RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return shop;
    }

    /**
     * 获取锁
     *
     * @param key
     * @return
     */
    private boolean tryLock(String key) {
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);

    }

    /**
     * 释放锁
     *
     * @param key
     * @return
     */
    private boolean delLock(String key) {
        Boolean aBoolean = stringRedisTemplate.delete(key);
        return BooleanUtil.isTrue(aBoolean);

    }
}
