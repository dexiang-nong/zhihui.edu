package com.tianji.promotion.service.impl.before.impl;//package com.tianji.promotion.service.impl.before.impl;
//
//import cn.hutool.core.bean.BeanUtil;
//import cn.hutool.core.bean.copier.CopyOptions;
//import cn.hutool.core.collection.CollUtil;
//import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.tianji.common.autoconfigure.redisson.annotations.Lock;
//import com.tianji.common.domain.dto.PageDTO;
//import com.tianji.common.exceptions.BadRequestException;
//import com.tianji.common.exceptions.BizIllegalException;
//import com.tianji.common.utils.UserContext;
//import com.tianji.promotion.constants.MQConstant;
//import com.tianji.promotion.constants.RedisConstant;
//import com.tianji.promotion.domain.dto.UserCouponDTO;
//import com.tianji.promotion.domain.enums.ExchangeCodeStatus;
//import com.tianji.promotion.domain.enums.UserCouponStatus;
//import com.tianji.promotion.domain.po.Coupon;
//import com.tianji.promotion.domain.po.ExchangeCode;
//import com.tianji.promotion.domain.po.UserCoupon;
//import com.tianji.promotion.domain.query.UserCouponQuery;
//import com.tianji.promotion.domain.vo.CouponVO;
//import com.tianji.promotion.mapper.CouponMapper;
//import com.tianji.promotion.mapper.UserCouponMapper;
//import com.tianji.promotion.service.IExchangeCodeService;
//import com.tianji.promotion.service.impl.before.IUserCouponService;
//import com.tianji.promotion.utils.CodeUtil;
//import lombok.RequiredArgsConstructor;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * <p>
// * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
// * </p>
// *
// * @author 張德帥
// * @since 2025-05-08
// *
// * @remark 这是未优化前的业务实现；由Lua脚本+RabbitMQ优化
// */
//@Service
//@RequiredArgsConstructor
//public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {
//
//    private final CouponMapper couponMapper;
//
//    private final IExchangeCodeService exchangeCodeService;
//
//    private final StringRedisTemplate redisTemplate;
//
////    private final RedissonClient redissonClient;
//
//    private final RabbitTemplate rabbitTemplate;
//
//    /**
//     * 领取发放中的优惠卷
//     */
//    @Lock(name = "lock:coupon:#{couponId}")
//    @Override
//    public void receiveCoupon(Long couponId) {
//        // 1. 查询优惠卷
////        Coupon coupon = couponMapper.selectById(couponId);
//        Coupon coupon = queryCouponCache(couponId);
//        if (coupon == null) {
//            throw new BadRequestException("优惠卷不存在！");
//        }
//        // 2. 校验发放时间
//        LocalDateTime now = LocalDateTime.now();
//        if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
//            throw new BizIllegalException("优惠券活动未开始或已经结束");
//        }
//        // 3. 校验库存
//        if (coupon.getIssueNum() >= coupon.getTotalNum()) {
//            throw new BadRequestException("优惠卷库存不足！");
//        }
//        // 4. 校验限领数量
//        String limitKey = RedisConstant.COUPON_LIMIT_KEY_PREFIX + couponId;
//        Long userId = UserContext.getUser();
//        Long count = redisTemplate.opsForHash().increment(limitKey, userId.toString(), 1);
//        if (count > coupon.getUserLimit()) {
//            throw new BadRequestException("超过领取数量");
//        }
//        // 5. 扣减优惠卷库存
//        String cacheKey = RedisConstant.COUPON_CACHE_KEY_PREFIX + couponId;
//        redisTemplate.opsForHash().increment(cacheKey, "totalNum", -1);
//        // 6. 发送MQ (更新发放数量、创建用户卷)
//        rabbitTemplate.convertAndSend(
//                MQConstant.Exchange.PROMOTION_EXCHANGE,
//                MQConstant.Key.COUPON_RECEIVE,
//                UserCouponDTO.builder()
//                        .couponId(couponId)
//                        .userId(userId)
//                        .build()
//        );
//
//        // 4. 校验优惠卷，并生成用户卷记录
////        LockSaveUserCoupon(coupon, UserContext.getUser(), null);
//        /*
//            非事务方法调用事务方法, 内部用this调用, 导致事务失效
//            解决: 获取代理对象, 由代理对象调用事务方法, 事务生效
//         */
////        IUserCouponService proxy = (IUserCouponService) AopContext.currentProxy();
////        proxy.checkCouponAndSaveUserCoupon(coupon, UserContext.getUser(), null);
//    }
//
//    /**
//     * 兑换码兑换优惠券接口
//     */
//    @Lock(name = "lock:coupon:#{T(com.tianji.common.utils.UserContext).getUser()}")
//    @Override
//    public void exchangeCoupon(String code) {
//        // 1. 校验兑换码并获取兑换码ID
//        long serialNum = CodeUtil.parseCode(code);
//        try {
//            // 2. 校验是否已经兑换 (直接设置, 成功返回0)
//            Boolean success = redisTemplate.opsForValue()
//                    .setBit(RedisConstant.COUPON_CODE_MAP_KEY, serialNum, true);
//            if (Boolean.TRUE.equals(success)) {
//                throw new BadRequestException("该兑换码已经兑换！");
//            }
//            // 3. 校验兑换码
//            Long couponId = exchangeCodeService.queryCouponIdBySerial(serialNum);
//            if (couponId == null) {
//                throw new BadRequestException("兑换码不存在！");
//            }
//            // 4. 校验发放时间
////            Coupon coupon = couponMapper.selectById(couponId);
//            Coupon coupon = queryCouponCache(couponId);
//            LocalDateTime now = LocalDateTime.now();
//            if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
//                throw new BizIllegalException("优惠券活动未开始或已经结束");
//            }
//            // 5. 校验限领数量
//            String limitKey = RedisConstant.COUPON_LIMIT_KEY_PREFIX + couponId;
//            Long userId = UserContext.getUser();
//            Long count = redisTemplate.opsForHash().increment(limitKey, userId.toString(), 1);
//            if (count > coupon.getUserLimit()) {
//                throw new BadRequestException("超过领取数量");
//            }
//            // 6. 发送MQ (更新发放数量、创建用户卷)
//            rabbitTemplate.convertAndSend(
//                    MQConstant.Exchange.PROMOTION_EXCHANGE,
//                    MQConstant.Key.COUPON_RECEIVE,
//                    UserCouponDTO.builder()
//                            .couponId(couponId)
//                            .userId(userId)
//                            .exchangeCodeId(serialNum)
//                            .build()
//            );
//
////            LockSaveUserCoupon(coupon, UserContext.getUser(), serialNum);
//            /*
//                非事务方法调用事务方法, 内部用this调用, 导致事务失效
//                解决: 获取代理对象, 由代理对象调用事务方法, 事务生效
//             */
////            IUserCouponService proxy = (IUserCouponService) AopContext.currentProxy();
////            proxy.checkCouponAndSaveUserCoupon(coupon, UserContext.getUser(), serialNum);
//        } catch (Exception e) {
//            // 重置BitMap
//            redisTemplate.opsForValue()
//                    .setBit(RedisConstant.COUPON_CODE_MAP_KEY, serialNum, false);
//            throw e;
//        }
//    }
//
////    /**
////     * 成功获取分布式锁，调用checkCouponAndSaveUserCoupon生成用户卷记录
////     */
////    private void LockSaveUserCoupon(Coupon coupon, Long userId, Long serialNum) {
//////        synchronized(userId.toString().intern()){ // 这里加锁，这样锁在事务之外
//////            IUserCouponService proxy = (IUserCouponService) AopContext.currentProxy();
//////            proxy.checkCouponAndSaveUserCoupon(coupon, userId, serialNum);
//////        }
////
////        RLock lock = redissonClient.getLock(RedisConstant.LOCK_PREFIX + userId);
////        if (!lock.tryLock()) {
////            throw new BadRequestException("请勿重复领取！");
////        }
////        try {
////            /*
////                非事务方法调用事务方法, 内部用this调用, 导致事务失效
////                解决: 获取代理对象, 由代理对象调用事务方法, 事务生效
////             */
////            IUserCouponService proxy = (IUserCouponService) AopContext.currentProxy();
////            proxy.checkCouponAndSaveUserCoupon(coupon, userId, serialNum);
////        } finally {
////            lock.unlock(); // 释放锁
////        }
////    }
//
////    /**
////     * 校验优惠卷，并生成用户卷记录
////     */
////    @Lock(name = "lock:coupon:#{userId}")
////    @Transactional // 将事务控制粒度下沉到具体业务方法中：事务边界更清晰、便于复用、符合“职责分离”原则
////    @Override
////    public void checkCouponAndSaveUserCoupon(Coupon coupon, Long userId, Long serialNum) {
////        // 1. 判断当前用户在该优惠卷上是否已经达到限令数量
////        Integer count = lambdaQuery()
////                .eq(UserCoupon::getUserId, userId)
////                .eq(UserCoupon::getCouponId, coupon.getId())
////                .count();
////        if (count >= coupon.getUserLimit()) {
////            throw new BadRequestException("已经达到领取数量！");
////        }
////        // 2. 生成用户卷记录
////        saveUserCoupon(userId, coupon);
////        /*
////            3. 更新优惠卷发放数量
////            update coupon set issue_num = issue_num + 1 where id = ? and issue_num < total_num ?（乐观锁）
////         */
////        int isSuccess = couponMapper.incrementIssueNum(coupon.getId());
////        if (isSuccess == 0) {
////            // 更新优惠卷发放数量失败，抛出异常，触发回滚
////            throw new BizIllegalException("优惠卷库存不足！");
////        }
////        // 4. 如果是兑换优惠卷, 更新兑换码状态
////        if (serialNum != null) {
////            exchangeCodeService.lambdaUpdate()
////                    .set(ExchangeCode::getUserId, userId)
////                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
////                    .eq(ExchangeCode::getId, serialNum)
////                    .update();
////        }
////    }
//
//    private Coupon queryCouponCache(Long couponId) {
//        String key = RedisConstant.COUPON_CACHE_KEY_PREFIX + couponId;
//        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
//        if (CollUtil.isEmpty(map)) {
//            return null;
//        }
//        return BeanUtil.mapToBean(map, Coupon.class, false, CopyOptions.create());
//    }
//
//    @Override
//    public void checkCouponAndSaveUserCoupon(UserCouponDTO userCouponDTO) {
//        // 1. 查询优惠卷
//        Coupon coupon = couponMapper.selectById(userCouponDTO.getCouponId());
//        if (coupon == null) {
//            throw new RuntimeException("优惠卷不存在！");
//        }
//        /*
//            2. 更新优惠卷发放数量
//            update coupon set issue_num = issue_num + 1 where id = ? and issue_num < total_num ?（乐观锁）
//         */
//        int success = couponMapper.incrementIssueNum(coupon.getId());
//        if (success == 0) {
//            throw new RuntimeException("优惠卷库存不足！");
//        }
//        // 3. 保存用户卷记录
//        Long userId = userCouponDTO.getUserId();
//        saveUserCoupon(userId, coupon);
//        // 4. 修改兑换码状态
//        Long exchangeCodeId = userCouponDTO.getExchangeCodeId();
//        if (exchangeCodeId != null) {
//            exchangeCodeService.lambdaUpdate()
//                    .set(ExchangeCode::getUserId, userId)
//                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
//                    .eq(ExchangeCode::getId, exchangeCodeId)
//                    .update();
//        }
//    }
//
//    /**
//     * 查询我的优惠卷
//     */
//    @Override
//    public PageDTO<CouponVO> queryMyCoupons(UserCouponQuery query) {
//        // 1. 分页查询我的优惠卷
//        Integer status = query.getStatus();
//        Page<UserCoupon> page = lambdaQuery()
//                .eq(status != null, UserCoupon::getStatus, UserCouponStatus.of(status))
//                .eq(UserCoupon::getUserId, UserContext.getUser())
//                .page(query.toMpPageDefaultSortByCreateTimeDesc());
//        List<UserCoupon> list = page.getRecords();
//        if (CollUtil.isEmpty(list)) {
//            return PageDTO.empty(page);
//        }
//        // 2. 获取优惠卷ID，查询优惠卷
//        Set<Long> couponIds = list.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
//        List<Coupon> couponList = couponMapper.selectBatchIds(couponIds);
//        // 3. 转换VO
//        List<CouponVO> voList = BeanUtil.copyToList(couponList, CouponVO.class);
//
//        return PageDTO.of(page, voList);
//    }
//
//    /**
//     * 查询即将过期的优惠卷
//     */
//    @Override
//    public List<UserCoupon> queryWillExpireCoupons(int timeout, ChronoUnit chronoUnit, int pageNo, int pageSize) {
//        // 分页查询即将过期的用户优惠卷
//        LocalDateTime now = LocalDateTime.now();
//        Page<UserCoupon> page = lambdaQuery()
//                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED)
//                .between(UserCoupon::getTermEndTime, now, now.plus(timeout, chronoUnit))
//                .page(new Page<>(pageNo, pageSize));
//        List<UserCoupon> list = page.getRecords();
//        if (CollUtil.isEmpty(list)) {
//            return Collections.emptyList();
//        }
//        return list;
//    }
//
//    /**
//     * 新增用户卷
//     */
//    @Override
//    public void saveUserCoupon(Long UserId, Coupon coupon) {
//        UserCoupon entity = new UserCoupon();
//        entity.setUserId(UserId);
//        entity.setCouponId(coupon.getId());
//        LocalDateTime termBeginTime = coupon.getTermBeginTime();
//        LocalDateTime termEndTime = coupon.getTermEndTime();
//        if (termBeginTime == null) {
//            termBeginTime = LocalDateTime.now();
//            termEndTime = termBeginTime.plusDays(coupon.getTermDays());
//        }
//        entity.setTermBeginTime(termBeginTime);
//        entity.setTermEndTime(termEndTime);
//        save(entity);
//    }
//
//}
