package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.constants.RedisConstant;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.vo.ExchangeCodeVO;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.utils.CodeUtil;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-05
 */
@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {
    
    /**
     * @介绍 BoundValueOperations 是 Spring Framework 中用于操作绑定到某个 key 的值的操作接口
     * @作用 主要用于与 Redis 进行交互时提供便捷的方法来操作数据
     * @具体作用描述 通过使用 BoundValueOperations，你可以对 Redis 中存储的值进行各种操作而无需每次都指定 key。一旦你绑定了一个 key 到 BoundValueOperations 实例上，所有对该实例的操作都将应用于这个特定的 key。这样可以简化代码，并使你的操作更加清晰和简洁
     */
    private final BoundValueOperations<String, String> serialOps;
    
    private final StringRedisTemplate redisTemplate;
    
    public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.serialOps = redisTemplate.boundValueOps(RedisConstant.COUPON_CODE_SERIAL_KEY);
    }
    
    /**
     * 异步保存兑换码
     */
    @Async("generateExchangeCodeExecutor") // 使用自己定义的线程池
    @Transactional
    @Override
    public void asyncGenerateCode(Coupon coupon) {
        Long couponId = coupon.getId();
        Integer totalNum = coupon.getTotalNum(); // 发放数量
        LocalDateTime issueEndTime = coupon.getIssueEndTime();
        // 1. 获取Redis自增序列号
        Long result = serialOps.increment(totalNum);
        if (result == null) {
            return;
        }
        int maxSerialNum = result.intValue();
        // 2. 创建兑换码实体, 存入数据库
        List<ExchangeCode> list = new ArrayList<>(maxSerialNum);
        for (int serialNum = maxSerialNum - totalNum + 1; serialNum <= maxSerialNum; serialNum++) {
            // 生成兑换码
            String code = CodeUtil.generateCode(serialNum, couponId);
            // 封装实体数据
            ExchangeCode entity = new ExchangeCode();
            list.add(entity);
            entity.setId(serialNum);
            entity.setCode(code);
            entity.setExchangeTargetId(couponId);
            entity.setExpiredTime(issueEndTime);
        }
        saveBatch(list);
        // 3. 写入Redis缓存（标记兑换码范围）
        redisTemplate.opsForZSet().add(RedisConstant.COUPON_RANGE_KEY, couponId.toString(), maxSerialNum);
    }
    
    /**
     * 分页查询兑换码
     */
    @Override
    public PageDTO<ExchangeCodeVO> queryCodePage(CodeQuery query) {
        // 1. 分页查询兑换码
        Page<ExchangeCode> page = lambdaQuery()
                .select(ExchangeCode::getId, ExchangeCode::getCode)
                .eq(ExchangeCode::getExchangeTargetId, query.getCouponId())
                .eq(ExchangeCode::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<ExchangeCode> list = page.getRecords();
        if (CollUtil.isEmpty(list)) {
            return PageDTO.empty(page);
        }
        // 2. 转换VO
        List<ExchangeCodeVO> voList = BeanUtil.copyToList(list, ExchangeCodeVO.class);
        return PageDTO.of(page, voList);
    }
    
    @Override
    public Long queryCouponIdBySerial(long serialNum) {
        Set<String> results = redisTemplate.opsForZSet()
                .rangeByScore(
                        RedisConstant.COUPON_RANGE_KEY,
                        serialNum, serialNum + 5000, // 5000是兑换码的上限
                        0L, 1L
                );
        if (CollUtil.isEmpty(results)) {
            return null;
        }
        return Long.valueOf(results.iterator().next());
    }
}
