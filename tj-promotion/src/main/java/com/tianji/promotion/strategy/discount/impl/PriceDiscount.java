package com.tianji.promotion.strategy.discount.impl;

import com.tianji.common.utils.NumberUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.strategy.discount.Discount;
import lombok.RequiredArgsConstructor;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-05
 * @Description: 满减类型规则
 */
@RequiredArgsConstructor
public class PriceDiscount implements Discount {
    
    private static final String RULE_TEMPLATE = "满{}减{}";
    
    
    @Override
    public boolean canUse(int totalAmount, Coupon coupon) {
        return totalAmount >= coupon.getThresholdAmount();
    }
    
    @Override
    public int calculateDiscount(int totalAmount, Coupon coupon) {
        return coupon.getDiscountValue();
    }
    
    @Override
    public String getRule(Coupon coupon) {
        return StringUtils.format(
                RULE_TEMPLATE,
                NumberUtils.scaleToStr(coupon.getThresholdAmount(), 2),
                NumberUtils.scaleToStr(coupon.getDiscountValue(), 2)
        );
    }
}
