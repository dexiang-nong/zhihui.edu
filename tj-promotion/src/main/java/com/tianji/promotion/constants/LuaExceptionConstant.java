package com.tianji.promotion.constants;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-15
 * @Description: Lua异常常量
 */
public interface LuaExceptionConstant {
    
    String[] RECEIVE_COUPON_ERROR = {
            "优惠卷不存在！",
            "优惠卷库存不足！",
            "优惠卷发放时间尚未开始或已经结束！",
            "已经达到领取数量"
    };
    
    String[] EXCHANGE_COUPON_ERROR = {
            "该兑换码已经兑换！",
            "兑换码不存在！",
            "优惠卷不存在！",
            "兑换码已经过期",
            "已经达到领取数量"
    };
    
}
