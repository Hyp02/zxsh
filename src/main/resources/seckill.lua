-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]
-- 订单id
local orderId = ARGV[3]
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
-- 保存下单用户 set
redis.call("sadd", orderKey, userId)
-- 将订单信息存放到stream.orders消息队列中【XADD】
redis.call('xadd','stream.orders','*',"userId",
        userId,"voucherId",voucherId,"id",orderId)


-- 成功返回0
return 0
