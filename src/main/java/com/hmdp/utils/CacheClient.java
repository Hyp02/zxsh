package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.RedisDate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author Han
 * @data 2023/9/11
 * @apiNode
 */
@Component
@Slf4j
public class CacheClient {
    // 缓存重建执行程序
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void setValue(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicExpired(String key, Object value, Long time, TimeUnit unit) {
        RedisDate redisDate = new RedisDate();
        redisDate.setExpiredTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        redisDate.setData(value);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisDate));
    }

    public <T, ID> T queryWithPassThorough(String KeyPrefix, ID id, Class<T> tClass,
                                           Long time, TimeUnit unit, Function<ID, T> dbMethod) {
        // 查询redis【这里的数据类型可选择Hash和String(这次选择String)】
        String key = KeyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        // 命中 返回
        if (StringUtils.isNotBlank(json)) {
            // 转换为对象后返回
            return JSONUtil.toBean(json, tClass);
        }
        // 如果是写入的空缓存
        if (json == "") {
            // 结束
            return null;
        }
        // 未命中 在数据库中查
        // 数据库中不存在 返回错误信息【getById是接口中的方法】
        T t = dbMethod.apply(id);
        if (t == null) {
            // 缓存穿透解决 写入空值
            stringRedisTemplate.opsForValue().set(key, "",
                    RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        String toJsonShop = JSONUtil.toJsonStr(t);
        // 数据库中存在 先保存在redis中再返回
        // 设置店铺缓存失效时间
        this.setValue(key, t, time, unit);

        return t;
    }

    /**
     * 逻辑过期解决缓存击穿
     *
     * @param id
     * @return
     */
    public <T, ID> T queryWithExpiredTime(String keyPrefix, ID id, Class<T> tClass,
                                          Function<ID, T> dbMethod, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 查询redis【这里的数据类型可选择Hash和String(这次选择String)】
        String json = stringRedisTemplate.opsForValue().get(key);
        // 未命中 直接返回
        if (StringUtils.isBlank(json)) {
            return null;
        }
        // 命中 查看这个缓存的逻辑过期时间是否过期
        // 反序列化shopJson字符串
        RedisDate redisDate = JSONUtil.toBean(json, RedisDate.class);
        JSONObject jsonObject = (JSONObject) redisDate.getData();
        T t = JSONUtil.toBean(jsonObject, tClass);
        LocalDateTime expiredTime = redisDate.getExpiredTime();
        // 未过期，直接返回店铺信息
        if (expiredTime.isAfter(LocalDateTime.now())) {
            return t;
        }
        // 已过期
        // 缓存重建
        // 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 成功
        if (isLock) {
            // TODO 开启新线程，缓存重建
            // 成功
            // 重建缓存 开启独立线程重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查数据库
                    T apply = dbMethod.apply(id);
                    // 设置逻辑过期
                    this.setWithLogicExpired(key, apply, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    delLock(lockKey);
                }
            });
        }
        // 直接返回旧缓存
        return t;
    }

    /**
     * 解决缓存穿透查询店铺函数
     * 互斥锁解决缓存击穿
     *
     * @param id 传入商铺id,用来获取Redis的key
     * @return
     */
    private <T,ID> T queryWithMutex(String keyPrefix,ID id,Class<T> tClass,Function<ID,T> dbMethod) {
        String key = keyPrefix+id;
        // 查询redis【这里的数据类型可选择Hash和String(这次选择String)】
        String json = stringRedisTemplate.opsForValue().get(key);
        // 命中 返回
        if (StringUtils.isNotBlank(json)) {
            // 转换为对象后返回
            return JSONUtil.toBean(json, tClass);
        }
        // 如果是写入的空缓存
        if ("".equals(json)) {
            // 结束
            return null;
        }
        // 未命中 获取互斥锁,
        T t = null;
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        try {
            boolean isLock = tryLock(lockKey);
            // 是否获取到了锁
            if (!isLock) {
                // 未获得 休眠
                Thread.sleep(50);
                // 递归 重新查询缓存
                return queryWithMutex(keyPrefix,id,tClass,dbMethod);
            }
            // 获得了锁 重建缓存
            t = dbMethod.apply(id);
            Thread.sleep(200);
            if (t == null) {
                // 缓存穿透解决 写入空值
                stringRedisTemplate.opsForValue().set(key, "",
                        RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            String toJsonShop = JSONUtil.toJsonStr(t);
            // 数据库中存在 先保存在redis中再返回
            // 设置店铺缓存失效时间
            stringRedisTemplate.opsForValue().set(key, toJsonShop,
                    RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
            // 释放互斥锁
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放锁
            delLock(lockKey);
        }
        // 返回信息
        return t;
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
