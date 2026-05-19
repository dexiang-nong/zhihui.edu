package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 学习记录表
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("learning_record")
@Schema(name="LearningRecord对象", description="学习记录表")
public class LearningRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description =  "学习记录的id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description =  "对应课表的id")
    private Long lessonId;

    @Schema(description =  "对应小节的id")
    private Long sectionId;

    @Schema(description =  "用户id")
    private Long userId;

    @Schema(description =  "视频的当前观看时间点，单位秒")
    private Integer moment;

    @Schema(description =  "是否完成学习，默认false")
    private Boolean finished;

    @Schema(description =  "第一次观看时间")
    private LocalDateTime createTime;

    @Schema(description =  "完成学习的时间")
    private LocalDateTime finishTime;

    @Schema(description =  "更新时间（最近一次观看时间）")
    private LocalDateTime updateTime;


}
