package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * id生成
 *
 * @author Han
 * @data 2023/9/14
 * @apiNode
 */
@Component
public class RedisIdWorker {
    //开始时间
    private static final long BEGIN_TIME = 1672531200L;
    // 注入
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        long l = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println(l);
    }

    /**
     * 获取订单id
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix) {
        // 获取时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long time = nowSecond - BEGIN_TIME;
        // 生成序列号
        // 获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + date);
        // 将时间戳左移32位，将生成的count拼接
        return time << 32 | count;
    }

}

