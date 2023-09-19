package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.apache.ibatis.javassist.ClassPath;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author Han
 * @data 2023/9/18
 * @apiNode
 */
public class SimpleRedisLock implements ILock {

    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final String KEY_PREFIX = "lock:";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

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
        // 获取线程id，加上id前缀
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 加锁
        Boolean isBool = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, expiredTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(isBool);
    }

    /**
     * 释放锁
     * 优化，防止删除不属于自己的锁 使用lua脚本
     * 1.第一个线程进来 阻塞 锁超时释放
     * 2.第二个进来 因为锁超时释放拿到锁
     * 3.这时，第一个线程阻塞完毕，因为在2中 第二个线程进来将ARGV设置成了第二个线程的id，
     * 4 这时候1线程阻塞完毕，进行删锁时，发现key对应的值不同，所以不会发生误删
     */
    @Override
    public void delLock() {
        //
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId()
        );
    }
    //**
    // * 释放锁
    // * 优化，防止删除不属于自己的锁
    // */

    // @Override
    // public void delLock() {
    //    // 获取uuid+线程id
    //    String threadId = ID_PREFIX + Thread.currentThread().getId();
    //    // 获取redis中的id
    //    String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
    //    // 判断是否是自己的锁
    //    // 比较当前线程id和redis中的值是否相同
    //    if (threadId.equals(id)) {
    //        stringRedisTemplate.delete(KEY_PREFIX + name);
    //    }
    //
    //}
}
