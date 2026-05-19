package com.tianji.promotion.constants;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-06
 * @Description: Redis常量
 */
public interface RedisConstant {
    
    /** 兑换码自增ID的KEY */
    String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";
    
    /** 优惠券范围KEY */
    String COUPON_RANGE_KEY = "coupon:range";
    
    /** 兑换码KEY */
    String COUPON_CODE_MAP_KEY = "coupon:code:map";
    
    /** 标识优惠卷短信已经提醒前缀: sms:remind:{userCouponId} */
    String SMS_REMIND_KEY_PREFIX = "sms:remind:";
    
    /** 分布式锁前缀 */
    String LOCK_PREFIX = "lock:coupon:";
    
    /** 优惠卷缓存KEY前缀: coupon:cache:{couponId} */
    String COUPON_CACHE_KEY_PREFIX = "coupon:cache:";
    
    /** 优惠卷用户限领数量: coupon:limit:{couponId} */
    String COUPON_LIMIT_KEY_PREFIX = "coupon:limit:";
    
}
