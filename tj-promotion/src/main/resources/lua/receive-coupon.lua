local cacheKey = KEYS[1] -- 优惠卷缓存KEY (Hash)
local limitKey = KEYS[2] -- 用户领取数量KEY (Hash)
local userId = ARGV[1]   -- 用户ID
-- 1. 优惠卷是否存在
if (redis.call('EXISTS', cacheKey) == 0) then
    return 1
end
-- 2. 库存是否充足
if (tonumber(redis.call('HGET', cacheKey, 'totalNum')) < 1) then
    return 2
end
-- 3. 是否在发放时间内
local now = tonumber(redis.call('time')[1]); -- 当前时间戳（秒）
if (
    now < tonumber(redis.call('HGET', cacheKey, 'issueBeginTime')) -- 未开始
    or
    now > tonumber(redis.call('HGET', cacheKey, 'issueEndTime'))   -- 已结束
) then
    return 3
end
-- 4. 用户是否可以领取
if (tonumber(redis.call('HGET', cacheKey, 'userLimit')) < tonumber(redis.call('HINCRBY', limitKey, userId, 1))) then
    return 4
end
-- 5. 扣减库存
redis.call('HINCRBY', cacheKey, 'totalNum', -1)
-- 6. 返回成功标识
return 0