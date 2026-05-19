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
 * 互动问题的回答或评论
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("interaction_reply")
@Schema(name="InteractionReply对象", description="互动问题的回答或评论")
public class InteractionReply implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description =  "互动问题的回答id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description =  "互动问题问题id")
    private Long questionId;

    @Schema(description =  "回复的上级回答id")
    private Long answerId;

    @Schema(description =  "回答者id")
    private Long userId;

    @Schema(description =  "回答内容")
    private String content;

    @Schema(description =  "回复的目标用户id")
    private Long targetUserId;

    @Schema(description =  "回复的目标回复id")
    private Long targetReplyId;

    @Schema(description =  "评论数量")
    private Integer replyTimes;

    @Schema(description =  "点赞数量")
    private Integer likedTimes;

    @Schema(description =  "是否被隐藏，默认false")
    private Boolean hidden;

    @Schema(description =  "是否匿名，默认false")
    private Boolean anonymity;

    @Schema(description =  "创建时间")
    private LocalDateTime createTime;

    @Schema(description =  "更新时间")
    private LocalDateTime updateTime;


}
