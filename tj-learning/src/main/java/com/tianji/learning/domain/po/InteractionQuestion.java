package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianji.learning.domain.enums.QuestionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 互动提问的问题表
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("interaction_question")
@Schema(name="InteractionQuestion对象", description="互动提问的问题表")
public class InteractionQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键，互动问题的id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "互动问题的标题")
    private String title;

    @Schema(description = "问题描述信息")
    private String description;

    @Schema(description = "所属课程id")
    private Long courseId;

    @Schema(description = "所属课程章id")
    private Long chapterId;

    @Schema(description = "所属课程节id")
    private Long sectionId;

    @Schema(description = "提问学员id")
    private Long userId;

    @Schema(description = "最新的一个回答的id")
    private Long latestAnswerId;

    @Schema(description = "问题下的回答数量")
    private Integer answerTimes;

    @Schema(description = "是否匿名，默认false")
    private Boolean anonymity;

    @Schema(description = "是否被隐藏，默认false")
    private Boolean hidden;

    @Schema(description = "管理端问题状态：0-未查看，1-已查看")
    private QuestionStatus status;

    @Schema(description = "提问时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;


}
