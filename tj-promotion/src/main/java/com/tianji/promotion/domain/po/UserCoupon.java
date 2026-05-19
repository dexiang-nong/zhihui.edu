package com.tianji.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianji.promotion.domain.enums.UserCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_coupon")
@Schema(name="UserCoupon对象", description="用户领取优惠券的记录，是真正使用的优惠券信息")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description =  "用户券id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description =  "优惠券的拥有者")
    private Long userId;

    @Schema(description =  "优惠券模板id")
    private Long couponId;

    @Schema(description =  "优惠券有效期开始时间")
    private LocalDateTime termBeginTime;

    @Schema(description =  "优惠券有效期结束时间")
    private LocalDateTime termEndTime;

    @Schema(description =  "优惠券使用时间（核销时间）")
    private LocalDateTime usedTime;

    @Schema(description =  "优惠券状态，1：未使用，2：已使用，3：已失效")
    private UserCouponStatus status;

    @Schema(description =  "创建时间")
    private LocalDateTime createTime;

    @Schema(description =  "更新时间")
    private LocalDateTime updateTime;


}
