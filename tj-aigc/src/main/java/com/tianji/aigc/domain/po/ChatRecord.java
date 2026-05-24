package com.tianji.aigc.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 对话记录表
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-23
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("chat_record")
@Schema(name="ChatRecord对象", description="对话记录表")
public class ChatRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "对话id")
    private String conversationId;

    @Schema(description = "对话数据")
    private String data;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private Long creater;

    @Schema(description = "更新人")
    private Long updater;


}
