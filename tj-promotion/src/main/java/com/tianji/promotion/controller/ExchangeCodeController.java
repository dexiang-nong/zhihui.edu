package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.vo.ExchangeCodeVO;
import com.tianji.promotion.service.IExchangeCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 兑换码 前端控制器
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-05
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "兑换码接口管理")
@RequestMapping("/codes")
public class ExchangeCodeController {
    
    private final IExchangeCodeService exchangeCodeService;
    
    /**
     * 分页查询兑换码
     */
    @Operation(summary ="分页查询兑换码")
    @GetMapping("/page")
    public PageDTO<ExchangeCodeVO> queryCodePage(CodeQuery query) {
        return exchangeCodeService.queryCodePage(query);
    }

}
