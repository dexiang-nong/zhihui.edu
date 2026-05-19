package com.tianji.api.dto.remark;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <p>
 *
 * </p>
 *
 * @author 張德帥
 * @version 1.0
 * @since 2026-05-07
 */
@Data
@Schema(description = "点赞记录表单实体")
public class LearningLikeRecordFromDTO {
    @Schema(description = "点赞业务id")
    @NotNull(message = "业务id不能为空")
    private Long id;
    
    @Schema(description = "是否点赞，true：点赞；false：取消点赞")
    @NotNull(message = "是否点赞不能为空")
    private Boolean liked;
}
