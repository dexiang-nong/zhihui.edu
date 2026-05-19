package com.tianji.promotion.listener;

import com.tianji.promotion.constants.MQConstant;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-15
 * @Description: 优惠卷监听器
 */
@Component
@RequiredArgsConstructor
public class CouponListener {
    
    private final IUserCouponService userCouponService;
    
    /**
     * 生成用户卷记录
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MQConstant.Queue.COUPON_RECEIVE_QUEUE, durable = "true"),
            exchange = @Exchange(value = MQConstant.Exchange.PROMOTION_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MQConstant.Key.COUPON_RECEIVE)
    )
    public void generateUserCoupon(UserCouponDTO userCouponDTO) {
        userCouponService.checkCouponAndSaveUserCoupon(userCouponDTO);
    }
    
}
