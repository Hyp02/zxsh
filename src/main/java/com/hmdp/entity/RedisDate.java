package com.hmdp.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 给shop类增加一个字段
 * @author Han
 * @data 2023/9/11
 * @apiNode
 */
@Data
public class RedisDate {
    private LocalDateTime expiredTime;
    private Object data;
}
