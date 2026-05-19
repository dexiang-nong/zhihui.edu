package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-05
 */
public interface ICouponService extends IService<Coupon> {
    
    /**
     * 新增优惠卷
     */
    void saveCoupon(CouponFormDTO dto);
    
    /**
     * 分页查询优惠卷列表
     */
    PageDTO<CouponPageVO> queryCouponPage(CouponQuery query);
    
    /**
     * 修改优惠卷
     */
    void updateCoupon(CouponFormDTO dto);
    
    /**
     * 删除优惠卷
     */
    void removeCoupon(Long id);
    
    /**
     * 查看优惠卷（根据IO查询优惠卷）
     */
    CouponDetailVO getCoupon(Long id);
    
    /**
     * 发放优惠卷
     */
    void issueCoupon(CouponIssueFormDTO dto);
    
    /**
     * 暂停发放
     */
    void pauseCoupon(Long id);
    
    /**
     * 查询发放中的优惠卷
     */
    List<CouponVO> queryIssuedCoupon();
}
