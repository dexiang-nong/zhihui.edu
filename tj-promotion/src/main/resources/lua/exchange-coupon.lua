local rangeKey = KEYS[1]        -- 兑换码范围KEY（ZSet）
local codeMapKey = KEYS[2]      -- 兑换码是否兑换KEY（BitMap）
local cacheKeyPrefix = KEYS[3]  -- 优惠卷缓存KEY前缀（Hash）
local limitKeyPrefix = KEYS[4]  -- 用户领取数量KEY（Hash）
local codeId = ARGV[1]          -- 第一个兑换码ID
local maxCodeId = ARGV[2]       -- 兑换码ID最大范围
local userId = ARGV[3]          -- 用户ID
-- 1. 兑换码是否兑换
if (redis.call('GETBIT', codeMapKey, codeId) == 1) then
    return "1"
end
-- 2. 兑换码是否存在
local zSetResult = redis.call('ZRANGEBYSCORE', rangeKey, codeId, maxCodeId, 'LIMIT', 0, 1)
if (#zSetResult == 0) then
    return "2"
end
-- 3. 优惠卷是否存在
local couponId = zSetResult[1]              -- 优惠卷ID
local cacheKey = cacheKeyPrefix .. couponId -- 优惠卷缓存KEY
local limitKey = limitKeyPrefix .. couponId -- 用户领取数量KEY
if (redis.call('EXISTS', cacheKey) == 0) then
    return "3"
end
-- 4. 是否在发放时间内
local now = tonumber(redis.call('time')[1]); -- 当前时间戳（秒）
if (
    now < tonumber(redis.call('HGET', cacheKey, 'issueBeginTime')) -- 未开始
    or
    now > tonumber(redis.call('HGET', cacheKey, 'issueEndTime'))   -- 已结束
) then
    return "4"
end
-- 5. 用户是否还可以领取
if (tonumber(redis.call('HGET', cacheKey, 'userLimit')) < tonumber(redis.call('HINCRBY', limitKey, userId, 1))) then
    return "5"
end
-- 6. 标记兑换
redis.call('SETBIT', codeMapKey, codeId, 1)
-- 7. 返回优惠卷ID，后续生成用户卷使用
return couponId

-- 备注: 绝对不可以 `return tonumber(couponId)`, Lua脚本在返回一个大数会造成精度丢失