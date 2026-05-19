package com.tianji.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.promotion.domain.po.Coupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 Mapper 接口
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-05
 */
public interface CouponMapper extends BaseMapper<Coupon> {
    
    @Update("update coupon set issue_num = issue_num + 1 where id = #{id} and issue_num < total_num")
    int incrementIssueNum(@Param("id") Long id);
    
    int incrUsedNum(@Param("couponIds") List<Long> couponIds, @Param("increment") int increment);
}
