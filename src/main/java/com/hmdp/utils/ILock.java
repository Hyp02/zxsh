package com.hmdp.utils;

/**
 * @author Han
 * @data 2023/9/18
 * @apiNode
 */
public interface ILock {
    /**
     * 尝试获取锁
     * @param expiredTime
     * @return
     */
    boolean tryLock(long expiredTime);

    /**
     * 释放锁
     */
    void delLock();
}
