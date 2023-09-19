-- 判断要删除的锁的值是否与当前线程的值相同
if(redis.call('get',KEYS[1] ) == ARGV[1] ) then
    -- 释放锁
    return redis.call('del',KEYS[1])
end
return 0