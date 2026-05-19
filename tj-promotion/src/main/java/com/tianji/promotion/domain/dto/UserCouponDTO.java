package com.tianji.promotion.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-15
 * @Description: 用户卷DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponDTO {
    
    /**
     * 优惠卷ID
     */
    private Long couponId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 兑换码ID
     */
    private Long exchangeCodeId;
    
}