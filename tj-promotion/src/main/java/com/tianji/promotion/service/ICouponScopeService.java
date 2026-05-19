package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.po.CouponScope;

import java.util.List;

/**
 * <p>
 * 优惠券作用范围信息 服务类
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-05
 */
public interface ICouponScopeService extends IService<CouponScope> {
    
    /**
     * 保存优惠券作用范围
     */
    void saveCouponScopeList(Long couponId, List<Long> scopes);
    
    /**
     * 根据优惠卷ID删除优惠卷作用范围
     */
    void removeByCouponId(Long id);
    
    /**
     * 根据优惠卷ID查询优惠卷作用范围
     */
    List<CouponScope> getListByCouponId(Long id);
}
