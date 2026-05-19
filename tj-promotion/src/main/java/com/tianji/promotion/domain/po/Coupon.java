package com.tianji.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianji.promotion.domain.enums.CouponStatus;
import com.tianji.promotion.domain.enums.DiscountType;
import com.tianji.promotion.domain.enums.ObtainType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 优惠券的规则信息
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("coupon")
@Schema(description = "优惠券的规则信息")
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "优惠券id", example = "")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("`name`")
    @Schema(description = "优惠券名称，可以和活动名称保持一致")
    private String name;

    @Schema(description = "优惠券类型，1：普通券。目前就一种，保留字段", example = "")
    private Integer type;

    @Schema(description = "折扣类型：1：满减，2：每满减，3：折扣，4：无门槛", example = "")
    private DiscountType discountType;

    @TableField("`specific`")
    @Schema(description = "是否限定作用范围，false：不限定，true：限定。默认false", example = "")
    private Boolean specific;

    @Schema(description = "折扣值，如果是满减则存满减金额，如果是折扣，则存折扣率，8折就是存80", example = "")
    private Integer discountValue;

    @Schema(description = "使用门槛：0：表示无门槛，其他值：最低消费金额")
    private Integer thresholdAmount;

    @Schema(description = "最高优惠金额，满减最大，0：表示没有限制，不为0，则表示该券有金额的限制", example = "")
    private Integer maxDiscountAmount;

    @Schema(description = "获取方式：1：手动领取，2：兑换码", example = "")
    private ObtainType obtainWay;

    @Schema(description = "开始发放时间")
    private LocalDateTime issueBeginTime;

    @Schema(description = "结束发放时间", example = "")
    private LocalDateTime issueEndTime;

    @Schema(description = "优惠券有效期天数：0：表示有效期是指定有效期")
    private Integer termDays;

    @Schema(description = "优惠券有效期开始时间")
    private LocalDateTime termBeginTime;

    @Schema(description = "优惠券有效期结束时间", example = "")
    private LocalDateTime termEndTime;

    @Schema(description = "优惠券配置状态，1：待发放，2：未开始，  3：进行中，4：已结束，5：暂停")
    private CouponStatus status;

    @Schema(description = "总数量，不超过10000", example = "")
    private Integer totalNum;

    @Schema(description = "已发行数量，用于判断是否超发", example = "")
    private Integer issueNum;

    @Schema(description = "已使用数量")
    private Integer usedNum;

    @Schema(description = "每个人限领的数量，默认1", example = "")
    private Integer userLimit;

    @Schema(description = "拓展参数字段，保留字段")
    private String extParam;

    @Schema(description = "创建时间", example = "")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private Long creater;

    @Schema(description = "更新人")
    private Long updater;

}