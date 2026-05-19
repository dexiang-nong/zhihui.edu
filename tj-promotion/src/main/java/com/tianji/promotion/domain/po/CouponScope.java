package com.tianji.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 优惠券作用范围信息
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("coupon_scope")
@Schema(name="CouponScope对象", description="优惠券作用范围信息")
public class CouponScope implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description =  "范围限定类型：1-分类，2-课程，等等")
    private Integer type;

    @Schema(description =  "优惠券id")
    private Long couponId;

    @Schema(description =  "优惠券作用范围的业务id，例如分类id、课程id")
    private Long bizId;


}
