package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.ICouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 前端控制器
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-05
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠卷接口管理")
@RequestMapping("/coupons")
public class CouponController {
    
    private final ICouponService couponService;
    
    /**
     * 新增优惠卷
     */
    @Operation(summary = "新增优惠卷")
    @PostMapping
    public void addCoupon(@RequestBody @Valid CouponFormDTO dto) {
        couponService.saveCoupon(dto);
    }
    
    /**
     * 分页查询优惠卷列表
     */
    @Operation(summary ="分页查询优惠卷列表")
    @GetMapping("/page")
    public PageDTO<CouponPageVO> queryCouponPage(CouponQuery query) {
        return couponService.queryCouponPage(query);
    }
    
    /**
     * 发放优惠卷
     */
    @Operation(summary ="发放优惠卷")
    @PutMapping("/{id}/issue")
    public void issueCoupon(@Valid @RequestBody CouponIssueFormDTO dto) {
        couponService.issueCoupon(dto);
    }
    
    /**
     * 编辑优惠卷
     */
    @Operation(summary ="编辑优惠卷")
    @PutMapping("/{id}")
    public void editCoupon(@Valid @RequestBody CouponFormDTO dto) {
        couponService.updateCoupon(dto);
    }
    
    /**
     * 删除优惠卷
     */
    @Operation(summary ="删除优惠卷")
    @DeleteMapping("/{id}")
    public void deleteCoupon(@PathVariable("id") Long id) {
        couponService.removeCoupon(id);
    }
    
    /**
     * 查看优惠卷（根据IO查询优惠卷）
     */
    @Operation(summary ="查看优惠卷（根据IO查询优惠卷）")
    @GetMapping("/{id}")
    public CouponDetailVO getCoupon(@PathVariable("id") Long id) {
        return couponService.getCoupon(id);
    }
    
    /**
     * 暂停发放
     */
    @Operation(summary ="暂停发放")
    @PutMapping("/{id}/pause")
    public void pauseCoupon(@PathVariable("id") Long id) {
        couponService.pauseCoupon(id);
    }
    
    /**
     * 查询发放中的优惠卷
     */
    @Operation(summary ="查询发放中的优惠卷")
    @GetMapping("/list")
    public List<CouponVO> queryIssuedCoupon() {
        return couponService.queryIssuedCoupon();
    }
    
}
