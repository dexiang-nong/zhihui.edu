package com.tianji.promotion.service;

import com.tianji.promotion.domain.dto.CouponDiscountDTO;
import com.tianji.promotion.domain.dto.OrderCouponDTO;
import com.tianji.promotion.domain.dto.OrderCourseDTO;

import java.util.List;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-17
 * @Description: 优惠卷方案计算服务
 */
public interface IDiscountService {
    
    /**
     * 查询我的优惠券可用方案
     */
    List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses);
    
    /**
     * 根据券方案计算订单优惠明细
     */
    CouponDiscountDTO calculateDiscount(OrderCouponDTO orderCouponDTO);
}
