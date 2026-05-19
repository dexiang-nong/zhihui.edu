package com.tianji.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.promotion.domain.enums.UserCouponStatus;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 Mapper 接口
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-08
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
    
    /**
     * 查询当前用户所有可用优惠卷（将用户卷ID封装到creater中）
     */
    List<Coupon> queryMyConpons(Long userId);
    
    /**
     * 查询用户卷对应的优惠卷
     */
    List<Coupon> queryCouponByUserCouponIds(@Param("userCouponIds") List<Long> userCouponIds,
                                            @Param("status") UserCouponStatus status);
}
