package com.tianji.promotion.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-06
 * @Description: 兑换码VO
 */
@Data
@Schema(description = "兑换码VO")
public class ExchangeCodeVO {
    
    @Schema(description = "兑换码ID")
    private Long id;
    
    @Schema(description = "兑换码")
    private String code;
    
}
