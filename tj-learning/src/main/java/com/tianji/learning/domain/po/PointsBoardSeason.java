package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * <p>
 * 
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("points_board_season")
@Schema(name="PointsBoardSeason对象", description="")
public class PointsBoardSeason implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description =  "自增长id，season标示")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description =  "赛季名称，例如：第1赛季")
    private String name;

    @Schema(description =  "赛季开始时间")
    private LocalDate beginTime;

    @Schema(description =  "赛季结束时间")
    private LocalDate endTime;


}
