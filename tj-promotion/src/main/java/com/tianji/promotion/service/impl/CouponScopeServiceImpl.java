package com.tianji.promotion.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.mapper.CouponScopeMapper;
import com.tianji.promotion.service.ICouponScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券作用范围信息 服务实现类
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-05
 */
@Service
public class CouponScopeServiceImpl extends ServiceImpl<CouponScopeMapper, CouponScope> implements ICouponScopeService {
    
    /**
     * 保存优惠券作用范围
     */
    @Override
    @Transactional
    public void saveCouponScopeList(Long couponId, List<Long> scopes) {
        if (CollUtil.isEmpty(scopes)) {
            throw new BadRequestException("限定范围不能为空");
        }
        List<CouponScope> list = scopes.stream()
                .map(bizId -> new CouponScope().setBizId(bizId).setCouponId(couponId))
                .collect(Collectors.toList());
        saveBatch(list);
    }
    
    /**
     * 根据优惠卷ID删除优惠卷作用范围
     */
    @Override
    public void removeByCouponId(Long id) {
        lambdaUpdate()
                .eq(CouponScope::getCouponId, id)
                .remove();
    }
    
    /**
     * 根据优惠卷ID查询优惠卷作用范围
     */
    @Override
    public List<CouponScope> getListByCouponId(Long id) {
        return lambdaQuery()
                .eq(CouponScope::getCouponId, id)
                .list();
    }
}
