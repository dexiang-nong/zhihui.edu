package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponDiscountDTO;
import com.tianji.promotion.domain.dto.OrderCouponDTO;
import com.tianji.promotion.domain.dto.OrderCourseDTO;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.IDiscountService;
import com.tianji.promotion.service.IUserCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 前端控制器
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-08
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "用户领取优惠卷记录接口管理")
@RequestMapping("/user-coupons")
public class UserCouponController {
    
    private final IUserCouponService userCouponService;
    
    private final IDiscountService discountService;
    
    /**
     * 领取发放中的优惠卷
     */
    @Operation(summary ="领取优惠卷")
    @PostMapping("/{couponId}/receive")
    public void receiveCoupon(@PathVariable("couponId") Long couponId) {
        userCouponService.receiveCoupon(couponId);
    }
    
    /**
     * 兑换码兑换优惠券接口
     */
    @Operation(summary ="兑换优惠券")
    @PostMapping("/{code}/exchange")
    public void exchangeCoupon(@PathVariable("code") String code) {
        userCouponService.exchangeCoupon(code);
    }
    
    /**
     * 查询我的优惠卷
     */
    @Operation(summary ="查询我的优惠卷")
    @GetMapping("/page")
    public PageDTO<CouponVO> queryMyCoupons(UserCouponQuery query) {
        return userCouponService.queryMyCoupons(query);
    }
    
    /**
     * 查询我的优惠券可用方案
     */
    @Operation(summary ="查询我的优惠券可用方案")
    @PostMapping("/available")
    public List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> orderCourses){
        return discountService.findDiscountSolution(orderCourses);
    }
    
    /**
     * 根据券方案计算订单优惠明细
     */
    @Operation(summary ="根据券方案计算订单优惠明细")
    @PostMapping("/discount")
    public CouponDiscountDTO calculateDiscount(@RequestBody OrderCouponDTO orderCouponDTO){
        return discountService.calculateDiscount(orderCouponDTO);
    }
    
    /**
     * 核销指定优惠券
     */
    @Operation(summary ="核销指定优惠券")
    @PutMapping("/use")
    public void writeOffCoupon(@Parameter(description = "用户优惠券id集合") @RequestParam("couponIds") List<Long> userCouponIds){
        userCouponService.writeOffCoupon(userCouponIds);
    }
    
    /**
     * 退还指定优惠券
     */
    @Operation(summary ="退还指定优惠券")
    @PutMapping("/refund")
    public void refundCoupon(@Parameter(description = "用户优惠券id集合") @RequestParam("couponIds") List<Long> userCouponIds){
        userCouponService.refundCoupon(userCouponIds);
    }
    
    /**
     * 分页查询我的优惠券接口
     */
    @Operation(summary ="分页查询我的优惠券接口")
    @GetMapping("/rules")
    public List<String> queryDiscountRules(@Parameter(description = "用户优惠券id集合") @RequestParam("couponIds") List<Long> userCouponIds){
        return userCouponService.queryDiscountRules(userCouponIds);
    }

}
