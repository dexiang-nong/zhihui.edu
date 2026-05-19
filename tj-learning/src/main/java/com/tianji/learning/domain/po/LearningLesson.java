package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianji.learning.domain.enums.LessonStatus;
import com.tianji.learning.domain.enums.PlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("learning_lesson")
public class LearningLesson implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description =  "学员id")
    private Long userId;

    @Schema(description =  "课程id")
    private Long courseId;

    @Schema(description =  "课程状态，0-未学习，1-学习中，2-已学完，3-已失效")
    private LessonStatus status;

    @Schema(description =  "每周学习频率，例如每周学习6小节，则频率为6")
    private Integer weekFreq;

    @Schema(description =  "学习计划状态，0-没有计划，1-计划进行中")
    private PlanStatus planStatus;

    @Schema(description =  "已学习小节数量")
    private Integer learnedSections;

    @Schema(description =  "最近一次学习的小节id")
    private Long latestSectionId;

    @Schema(description =  "最近一次学习的时间")
    private LocalDateTime latestLearnTime;

    @Schema(description =  "创建时间")
    private LocalDateTime createTime;

    @Schema(description =  "过期时间")
    private LocalDateTime expireTime;

    @Schema(description =  "更新时间")
    private LocalDateTime updateTime;

}
