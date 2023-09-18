package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author Han
 * @data 2023/9/18
 * @apiNode
 */
public class SimpleRedisLock implements ILock {

    private static final String KEY_PREFIX = "lock:";
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private String name;

    /**
     * @param stringRedisTemplate 操作redis对象
     * @param key                 要获取锁的redis key
     */
    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String key) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = key;
    }

    /**
     * 尝试获取锁，使用redis实现分布锁
     *
     * @param expiredTime
     * @return
     */
    @Override
    public boolean tryLock(long expiredTime) {
        // 获取线程id
        long id = Thread.currentThread().getId();
        // 加锁
        Boolean isBool = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, id + "", expiredTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(isBool);
    }

    /**
     * 释放锁
     */
    @Override
    public void delLock() {
        stringRedisTemplate.delete(KEY_PREFIX + name);
    }
}
