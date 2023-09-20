-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]
-- 库存key
local stockKey = "seckill:stock:" .. voucherId
-- 订单key
local orderKey = "seckill:order:" .. voucherId
-- 判断库存是否充足
if (tonumber(redis.call("get", stockKey)) <= 0) then
    -- 库存不足
    return 1
end
-- 判断用户是否下过单
if (redis.call("sismember", orderKey, userId) == 1) then
    -- 下过单
    return 2
end
-- 扣减库存
redis.call("incrby", stockKey, -1)
-- 保存下单用户
redis.call("sadd", orderKey, userId)
-- 成功返回0
return 0
