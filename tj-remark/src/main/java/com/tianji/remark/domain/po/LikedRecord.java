package com.tianji.remark.domain.po;

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
 * 点赞记录表
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("liked_record")
@Schema(description = "点赞记录表")
public class LikedRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键id", example = "")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户id", example = "")
    private Long userId;

    @Schema(description = "点赞的业务id", example = "")
    private Long bizId;

    @Schema(description = "点赞的业务类型")
    private String bizType;

    @Schema(description = "创建时间", example = "")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "")
    private LocalDateTime updateTime;


}