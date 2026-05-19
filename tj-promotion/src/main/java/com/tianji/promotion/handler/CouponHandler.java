package com.tianji.promotion.handler;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.promotion.constants.RedisConstant;
import com.tianji.promotion.domain.enums.CouponStatus;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.service.ICouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-06
 * @Description: 优惠卷定时处理
 */
@Component
@RequiredArgsConstructor
public class CouponHandler {
    
    private final ICouponService couponService;
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 定时开始发放优惠卷
     */
    @XxlJob("TimingStartIssueCouponJob")
    public void timingStartIssueCouponJob() {
        // 定义分页参数
        int shardIndex = XxlJobHelper.getShardIndex(); // 获取执行器编号
        int shardTotal = XxlJobHelper.getShardTotal(); // 获取执行器数量
        int pageNo = shardIndex + 1; // 起始号就是分片序号+1
        int pageSize = 1000;
        while (true) {
            // 1. 分页查询未开始发放的优惠卷
            Page<Coupon> page = couponService.lambdaQuery()
                    .eq(Coupon::getStatus, CouponStatus.UN_ISSUE)
                    .le(Coupon::getIssueBeginTime, LocalDateTime.now())
                    .page(new Page<>(pageNo, pageSize));
            List<Coupon> list = page.getRecords();
            if (list.isEmpty()) {
                break;
            }
            // 2. 修改状态为发放中
            list = list.stream()
                    .map(entity -> entity.setStatus(CouponStatus.ISSUING))
                    .collect(Collectors.toList());
            // 3. 批量修改
            couponService.updateBatchById(list);
            // 4. 翻页
            pageNo += shardTotal;
        }
    }
    
    /**
     * 定时结束发放优惠卷
     */
    @XxlJob("TimingEndIssueCouponJob")
    public void timingEndIssueCouponJob() {
        // 定义分页参数
        int shardIndex = XxlJobHelper.getShardIndex(); // 获取执行器编号
        int shardTotal = XxlJobHelper.getShardTotal(); // 获取执行器数量
        int pageNo = shardIndex + 1; // 起始号就是分片序号+1
        int pageSize = 1000;
        // 循环处理
        while (true) {
            // 1. 分页查询发放中的优惠卷
            List<Coupon> list = couponService.lambdaQuery()
                    .eq(Coupon::getStatus, CouponStatus.ISSUING)
                    .le(Coupon::getIssueEndTime, LocalDateTime.now())
                    .page(new Page<>(pageNo, pageSize))
                    .getRecords();
            if (list.isEmpty()) {
                break;
            }
            // 2. 修改为发放结束
            list = list.stream()
                    .map(entity -> entity.setStatus(CouponStatus.FINISHED))
                    .collect(Collectors.toList());
            // 3. 批量修改
            couponService.updateBatchById(list);
            // 4. 翻页
            pageNo += shardTotal;
        }
    }
    
    /**
     * 定时删除过期优惠卷缓存
     */
    @XxlJob("TimingDeleteExpiredCouponCacheJob")
    public void timingDeleteExpiredCouponCacheJob() {
        // 定义分页参数
        int shardIndex = XxlJobHelper.getShardIndex(); // 获取执行器编号
        int shardTotal = XxlJobHelper.getShardTotal(); // 获取执行器数量
        int pageNo = shardIndex + 1; // 起始号就是分片序号+1
        int pageSize = 1000;
        
        while (true) {
            // 1. 查询过期优惠卷
            List<Coupon> list = couponService.lambdaQuery()
                    .select(Coupon::getId)
                    .le(Coupon::getTermEndTime, LocalDateTime.now())
                    .page(new Page<>(pageNo, pageSize))
                    .getRecords();
            if (list.isEmpty()) {
                break;
            }
            // 2. 提取优惠卷ID
            List<Long> couponIdList = list.stream()
                    .map(Coupon::getId)
                    .collect(Collectors.toList());
            // 3. 删除过期优惠卷缓存
            Set<String> keys = couponIdList.stream()
                    .map(couponId -> RedisConstant.COUPON_CACHE_KEY_PREFIX + couponId)
                    .collect(Collectors.toSet());
            redisTemplate.delete(keys);
            // 3. 翻页
            pageNo += shardTotal;
        }
    }
    
}
