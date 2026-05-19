package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 学霸天梯榜
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("points_board")
@Schema(name="PointsBoard对象", description="学霸天梯榜")
public class PointsBoard implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description =  "榜单id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description =  "学生id")
    private Long userId;

    @Schema(description =  "积分值")
    private Integer points;
    
    /*
         备注：由于要对points_board进行分表，所以下面两个字段不需要设计为数据库表
            1. rank，由ID替代
            2. season，由表名替代
     */

    @Schema(description =  "名次，只记录赛季前100")
    @TableField(exist = false)
    private Integer rank;

    @Schema(description =  "赛季，例如 1,就是第一赛季，2-就是第二赛季")
    @TableField(exist = false)
    private Integer season;


}
