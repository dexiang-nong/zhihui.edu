package com.tianji.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.vo.ExchangeCodeVO;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-05
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {
    
    /**
     * 异步保存兑换码
     */
    void asyncGenerateCode(Coupon coupon);
    
    /**
     * 分页查询兑换码
     */
    PageDTO<ExchangeCodeVO> queryCodePage(CodeQuery query);
    
    Long queryCouponIdBySerial(long serialNum);
}
