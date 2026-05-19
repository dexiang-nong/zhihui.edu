package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.LuaExceptionConstant;
import com.tianji.promotion.constants.MQConstant;
import com.tianji.promotion.constants.RedisConstant;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.enums.ExchangeCodeStatus;
import com.tianji.promotion.domain.enums.UserCouponStatus;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.strategy.DiscountStrategy;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-08
 */
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    private final CouponMapper couponMapper;
    
    private final StringRedisTemplate redisTemplate;

    private static final RedisScript<Long> RECEIVE_SCRIPT;

    private static final RedisScript<String> EXCHANGE_SCRIPT;

    private final RabbitTemplate rabbitTemplate;
    
    private final IExchangeCodeService exchangeCodeService;

    static {
        // 初始化`RECEIVE_SCRIPT` (领取优惠卷Lua脚本)
        RECEIVE_SCRIPT = RedisScript.of(new ClassPathResource("lua/receive-coupon.lua"), Long.class);
        // 初始化`EXCHANGE_SCRIPT` (兑换优惠卷Lua脚本)
        EXCHANGE_SCRIPT = RedisScript.of(new ClassPathResource("lua/exchange-coupon.lua"), String.class);
    }

    /**
     * 领取发放中的优惠卷
     */
    @Override
    public void receiveCoupon(Long couponId) {
        Long userId = UserContext.getUser();
        // 1. 执行Lua脚本
        Long result = redisTemplate.execute(
                RECEIVE_SCRIPT,
                List.of(
                        RedisConstant.COUPON_CACHE_KEY_PREFIX + couponId,
                        RedisConstant.COUPON_LIMIT_KEY_PREFIX + couponId
                ),
                userId.toString()
        );
        if (result != 0) {
            throw new BizIllegalException(LuaExceptionConstant.RECEIVE_COUPON_ERROR[result.intValue() - 1]);
        }
        // 2. 异步：更新优惠卷发放数量、保存用户卷记录
        rabbitTemplate.convertAndSend(
                MQConstant.Exchange.PROMOTION_EXCHANGE,
                MQConstant.Key.COUPON_RECEIVE,
                UserCouponDTO.builder()
                        .couponId(couponId)
                        .userId(userId)
                        .build()
        );
    }

    /**
     * 兑换码兑换优惠券接口
     */
    @Override
    public void exchangeCoupon(String code) {
        // 1. 校验兑换码并获取兑换码ID
        long serialNum = CodeUtil.parseCode(code);
        // 2. 执行Lua脚本
        Long userId = UserContext.getUser();
        String result = redisTemplate.execute(
                EXCHANGE_SCRIPT,
                List.of(
                        RedisConstant.COUPON_RANGE_KEY,
                        RedisConstant.COUPON_CODE_MAP_KEY,
                        RedisConstant.COUPON_CACHE_KEY_PREFIX,
                        RedisConstant.COUPON_LIMIT_KEY_PREFIX
                ),
                // 易错点: values参数不可以传递List
                String.valueOf(serialNum),
                String.valueOf(serialNum + 5000), // 5000是兑换码的上限
                userId.toString()
        );
        Long couponId = Long.valueOf(result);
        if (couponId < 10) {
            throw new BizIllegalException(LuaExceptionConstant.EXCHANGE_COUPON_ERROR[couponId.intValue() - 1]);
        }
        // 3. 异步：更新优惠卷发放数量、保存用户卷记录、修改兑换码状态
        rabbitTemplate.convertAndSend(
                MQConstant.Exchange.PROMOTION_EXCHANGE,
                MQConstant.Key.COUPON_RECEIVE,
                UserCouponDTO.builder()
                        .couponId(couponId)
                        .userId(userId)
                        .exchangeCodeId(serialNum)
                        .build()
        );
    }

    /**
     * 查询我的优惠卷
     */
    @Override
    public PageDTO<CouponVO> queryMyCoupons(UserCouponQuery query) {
        // 1. 分页查询我的优惠卷
        Integer status = query.getStatus();
        Page<UserCoupon> page = lambdaQuery()
                .select(UserCoupon::getCouponId) // 仅查询优惠卷ID
                .eq(status != null, UserCoupon::getStatus, status)
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<UserCoupon> list = page.getRecords();
        if (CollUtil.isEmpty(list)) {
            return PageDTO.empty(page);
        }
        // 2. 获取优惠卷ID，查询优惠卷
        Set<Long> couponIds = list.stream()
                .map(UserCoupon::getCouponId)
                .collect(Collectors.toSet());
        List<Coupon> couponList = couponMapper.selectBatchIds(couponIds);
        // 3. 转换VO
        List<CouponVO> voList = BeanUtil.copyToList(couponList, CouponVO.class);
        return PageDTO.of(page, voList);
    }

    /**
     * 查询即将过期的优惠卷
     */
    @Override
    public List<UserCoupon> queryWillExpireCoupons(int timeout, ChronoUnit chronoUnit, int pageNo, int pageSize) {
        // 分页查询即将过期的用户优惠卷
        LocalDateTime now = LocalDateTime.now();
        Page<UserCoupon> page = lambdaQuery()
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED)
                .between(UserCoupon::getTermEndTime, now, now.plus(timeout, chronoUnit))
                .page(new Page<>(pageNo, pageSize));
        List<UserCoupon> list = page.getRecords();
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }
    
    @Override
    public void checkCouponAndSaveUserCoupon(UserCouponDTO userCouponDTO) {
        // 1. 查询优惠卷
        Coupon coupon = couponMapper.selectById(userCouponDTO.getCouponId());
        if (coupon == null) {
            throw new RuntimeException("优惠卷不存在！");
        }
        /*
            2. 更新优惠卷发放数量
            update coupon set issue_num = issue_num + 1 where id = ? and issue_num < total_num ?（乐观锁）
         */
        int success = couponMapper.incrementIssueNum(coupon.getId());
        if (success == 0) {
            throw new RuntimeException("优惠卷库存不足！");
        }
        // 3. 保存用户卷记录
        Long userId = userCouponDTO.getUserId();
        saveUserCoupon(userId, coupon);
        // 4. 修改兑换码状态
        Long exchangeCodeId = userCouponDTO.getExchangeCodeId();
        if (exchangeCodeId != null) {
            exchangeCodeService.lambdaUpdate()
                    .set(ExchangeCode::getUserId, userId)
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                    .eq(ExchangeCode::getId, exchangeCodeId)
                    .update();
        }
    }
    
    /**
     * 新增用户卷
     */
    @Override
    public void saveUserCoupon(Long UserId, Coupon coupon) {
        UserCoupon entity = new UserCoupon();
        entity.setUserId(UserId);
        entity.setCouponId(coupon.getId());
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        if (termBeginTime == null) {
            termBeginTime = LocalDateTime.now();
            termEndTime = termBeginTime.plusDays(coupon.getTermDays());
        }
        entity.setTermBeginTime(termBeginTime);
        entity.setTermEndTime(termEndTime);
        save(entity);
    }

    /**
     * 核销指定优惠券
     */
    @Transactional
    @Override
    public void writeOffCoupon(List<Long> userCouponIds) {
        // 1. 查询优惠券
        List<UserCoupon> userCoupons = listByIds(userCouponIds);
        if (CollUtils.isEmpty(userCoupons)) {
            return;
        }
        // 2. 处理数据
        List<UserCoupon> list = userCoupons.stream()
                // 过滤无效券
                .filter(coupon -> {
                    if (coupon == null) {
                        return false;
                    }
                    if (UserCouponStatus.UNUSED != coupon.getStatus()) {
                        return false;
                    }
                    LocalDateTime now = LocalDateTime.now();
                    return !now.isBefore(coupon.getTermBeginTime()) && !now.isAfter(coupon.getTermEndTime());
                })
                // 组织新增数据
                .map(coupon -> {
                    UserCoupon c = new UserCoupon();
                    c.setId(coupon.getId());
                    c.setStatus(UserCouponStatus.USED);
                    return c;
                })
                .collect(Collectors.toList());

        // 4. 核销，修改优惠券状态
        boolean success = updateBatchById(list);
        if (!success) {
            return;
        }
        // 5. 更新已使用数量
        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .collect(Collectors.toList());
        int c = couponMapper.incrUsedNum(couponIds, 1);
        if (c < 1) {
            throw new DbException("更新优惠券使用数量失败！");
        }
    }

    /**
     * 退还指定优惠券
     */
    @Transactional
    @Override
    public void refundCoupon(List<Long> userCouponIds) {
        // 1. 查询优惠券
        List<UserCoupon> userCoupons = listByIds(userCouponIds);
        if (CollUtils.isEmpty(userCoupons)) {
            return;
        }
        // 2. 处理优惠券数据
        List<UserCoupon> list = userCoupons.stream()
                // 过滤无效券
                .filter(coupon -> coupon != null && UserCouponStatus.USED == coupon.getStatus())
                // 更新状态字段
                .map(coupon -> {
                    UserCoupon c = new UserCoupon();
                    c.setId(coupon.getId());
                    // 3. 判断有效期，是否已经过期，如果过期，则状态为 已过期，否则状态为 未使用
                    LocalDateTime now = LocalDateTime.now();
                    UserCouponStatus status = now.isAfter(coupon.getTermEndTime()) ?
                            UserCouponStatus.EXPIRED : UserCouponStatus.UNUSED;
                    c.setStatus(status);
                    return c;
                }).collect(Collectors.toList());

        // 4. 修改优惠券状态
        boolean success = updateBatchById(list);
        if (!success) {
            return;
        }
        // 5. 更新已使用数量
        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .collect(Collectors.toList());
        int c = couponMapper.incrUsedNum(couponIds, -1);
        if (c < 1) {
            throw new DbException("更新优惠券使用数量失败！");
        }
    }

    /**
     * 分页查询我的优惠券接口
     */
    @Override
    public List<String> queryDiscountRules(List<Long> userCouponIds) {
        // 1.查询优惠券信息
        List<Coupon> coupons = baseMapper.queryCouponByUserCouponIds(userCouponIds, UserCouponStatus.USED);
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }
        // 2.转换规则
        return coupons.stream()
                .map(c -> DiscountStrategy.getDiscount(c.getDiscountType()).getRule(c))
                .collect(Collectors.toList());
    }

}
