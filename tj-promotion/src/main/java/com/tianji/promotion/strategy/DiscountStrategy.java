package com.tianji.promotion.strategy;

import com.tianji.promotion.domain.enums.DiscountType;
import com.tianji.promotion.strategy.discount.Discount;
import com.tianji.promotion.strategy.discount.impl.NoThresholdDiscount;
import com.tianji.promotion.strategy.discount.impl.PerPriceDiscount;
import com.tianji.promotion.strategy.discount.impl.PriceDiscount;
import com.tianji.promotion.strategy.discount.impl.RateDiscount;

import java.util.EnumMap;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-05
 * @Description: 折扣策略的工厂，可以根据DiscountType枚举来获取某个折扣策略对象
 */
public class DiscountStrategy {
    
    private final static EnumMap<DiscountType, Discount> strategies;
    
    static {
        strategies = new EnumMap<>(DiscountType.class);
        strategies.put(DiscountType.NO_THRESHOLD, new NoThresholdDiscount());
        strategies.put(DiscountType.PER_PRICE_DISCOUNT, new PerPriceDiscount());
        strategies.put(DiscountType.RATE_DISCOUNT, new RateDiscount());
        strategies.put(DiscountType.PRICE_DISCOUNT, new PriceDiscount());
    }
    
    public static Discount getDiscount(DiscountType type) {
        return strategies.get(type);
    }
}
