package com.tianji.promotion.constants;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-15
 * @Description: MQ常量
 */
public interface MQConstant {
    
    interface Exchange {
        /**
         * 优惠卷有关的交换机
         */
        String PROMOTION_EXCHANGE = "promotion.topic";
    }
    
    interface Queue {
        /**
         * 用户卷队列
         */
        String COUPON_RECEIVE_QUEUE = "coupon.receive.queue";
    }
    
    interface Key {
        /** 领取优惠卷 */
        String COUPON_RECEIVE = "coupon.receive";
    }
    
}
