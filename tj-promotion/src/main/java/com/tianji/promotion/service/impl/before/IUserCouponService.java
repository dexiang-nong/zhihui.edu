package com.tianji.promotion.service.impl.before;//package com.tianji.promotion.service.impl.before;
//
//import com.baomidou.mybatisplus.extension.service.IService;
//import com.tianji.common.domain.dto.PageDTO;
//import com.tianji.promotion.domain.dto.UserCouponDTO;
//import com.tianji.promotion.domain.po.Coupon;
//import com.tianji.promotion.domain.po.UserCoupon;
//import com.tianji.promotion.domain.query.UserCouponQuery;
//import com.tianji.promotion.domain.vo.CouponVO;
//
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//
///**
// * <p>
// * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
// * </p>
// *
// * @author 張德帥
// * @since 2025-05-08
// */
//public interface IUserCouponService extends IService<UserCoupon> {
//
//    /**
//     * 领取发放中的优惠卷
//     */
//    void receiveCoupon(Long couponId);
//
//    /**
//     * 兑换码兑换优惠券接口
//     */
//    void exchangeCoupon(String code);
//
////    void checkCouponAndSaveUserCoupon(Coupon coupon, Long userId, Long serialNum);
//
//    void checkCouponAndSaveUserCoupon(UserCouponDTO userCouponDTO);
//
//    /**
//     * 查询我的优惠卷
//     */
//    PageDTO<CouponVO> queryMyCoupons(UserCouponQuery query);
//
//    /**
//     * 查询即将过期的优惠卷
//     */
//    List<UserCoupon> queryWillExpireCoupons(int timeout, ChronoUnit chronoUnit, int pageNo, int pageSize);
//
//    /**
//     * 新增用户卷
//     */
//    void saveUserCoupon(Long userId, Coupon coupon);
//
//}