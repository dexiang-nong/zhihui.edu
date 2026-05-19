package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CategoryBasicDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.RedisConstant;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.enums.CouponStatus;
import com.tianji.promotion.domain.enums.ObtainType;
import com.tianji.promotion.domain.enums.UserCouponStatus;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponScopeVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tianji.promotion.domain.enums.CouponStatus.*;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author 張德帥
 * @since 2025-05-05
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {
    
    private final ICouponScopeService couponScopeService;
    
    private final CategoryCache categoryCache;
    
    private final CourseClient courseClient;
    
    private final IExchangeCodeService exchangeCodeService;
    
    private final IUserCouponService userCouponService;
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 新增优惠卷
     */
    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        // 1. 转换实体
        Coupon entity = BeanUtil.toBean(dto, Coupon.class);
        // 2. 保存优惠卷数据
        save(entity);
        // 3. 判断是否有限定使用范围，如果有，需要保存优惠卷作用范围信息
        if (dto.getSpecific()) {
            couponScopeService.saveCouponScopeList(entity.getId(), dto.getScopes());
        }
    }
    
    /**
     * 分页查询优惠卷列表
     */
    @Override
    public PageDTO<CouponPageVO> queryCouponPage(CouponQuery query) {
        // 1. 分页查询优惠卷列表
        Integer type = query.getType();
        Integer status = query.getStatus();
        String name = query.getName();
        Page<Coupon> page = lambdaQuery()
                .eq(type != null, Coupon::getDiscountType, type)
                .eq(status != null, Coupon::getStatus, status)
                .like(StrUtil.isNotBlank(name), Coupon::getName, name)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<Coupon> list = page.getRecords();
        if (CollUtils.isEmpty(list)) {
            return PageDTO.empty(page);
        }
        // 2. 转换VO
        List<CouponPageVO> voList = BeanUtil.copyToList(list, CouponPageVO.class);
        return PageDTO.of(page, voList);
    }
    
    /**
     * 发放优惠卷
     */
    @Override
    @Transactional
    public void issueCoupon(CouponIssueFormDTO dto) {
        // 1. 查询优惠券
        Coupon coupon = getById(dto.getId());
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在！");
        }
        // 2. 判断优惠券状态，是否是暂停或待发放
        if (coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != PAUSE) {
            throw new BizIllegalException("优惠券状态错误！");
        }
        // 3. 判断是否是立刻发放
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime issueBeginTime = dto.getIssueBeginTime();
        boolean isBegin = issueBeginTime == null || issueBeginTime.isBefore(now);
        // 4. 更新优惠券
        // 4.1. 拷贝属性到PO
        Coupon entity = BeanUtils.copyBean(dto, Coupon.class);
        // 4.2. 更新状态
        if (isBegin) {
            entity.setStatus(ISSUING);
            entity.setIssueBeginTime(now); // 重新设置发放时间
        } else {
            entity.setStatus(UN_ISSUE);
        }
        // 4.3. 写入数据库
        updateById(entity);
        
        // 5. 如果领取方式是指定发放，并且状态是未发放（避免重复生成），需要生成兑换码
        if (coupon.getObtainWay() == ObtainType.ISSUE && coupon.getStatus() == CouponStatus.DRAFT) {
            coupon.setIssueEndTime(dto.getIssueEndTime()); // 设置发放结束时间
            exchangeCodeService.asyncGenerateCode(coupon);
        }
        // 6. 将优惠卷数据缓存到Redis中
        if (isBegin) {
            Map<String, Object> map = new HashMap<>();
            map.put("totalNum", coupon.getTotalNum().toString());
            map.put("userLimit", coupon.getUserLimit().toString());
            map.put("issueBeginTime", String.valueOf(DateUtils.toEpochMilli(entity.getIssueBeginTime()) / 1000));
            map.put("issueEndTime", String.valueOf(DateUtils.toEpochMilli(entity.getIssueEndTime()) / 1000));
            String key = RedisConstant.COUPON_CACHE_KEY_PREFIX + coupon.getId();
            redisTemplate.opsForHash().putAll(key, map);
        }
    }
    
    /**
     * 修改优惠卷
     */
    @Override
    @Transactional
    public void updateCoupon(CouponFormDTO dto) {
        // 1. 查询优惠卷，只有未发放的优惠卷可以修改
        Coupon coupon = getById(dto.getId());
        if (coupon.getStatus() != CouponStatus.DRAFT) {
            throw new IllegalArgumentException("优惠卷已发布，不能修改！");
        }
        // 2. 修改优惠卷
        Coupon entity = BeanUtil.toBean(dto, Coupon.class);
        updateById(entity);
        // 3. 如果是有限定范围修改为无限定，需要删除优惠卷作用范围信息
        if (coupon.getSpecific() && !entity.getSpecific()) {
            // 删除优惠卷作用范围信息
            couponScopeService.removeByCouponId(entity.getId());
        }
        // 4. 判断是否有限定使用范围，如果有，需要保存优惠卷作用范围信息
        if (dto.getSpecific()) {
            // 删除优惠卷作用范围信息 （需要删除旧的）
            couponScopeService.removeByCouponId(entity.getId());
            // 保存优惠卷作用范围信息
            couponScopeService.saveCouponScopeList(entity.getId(), dto.getScopes());
        }
    }
    
    /**
     * 删除优惠卷
     */
    @Override
    @Transactional
    public void removeCoupon(Long id) {
        // 1. 查询优惠卷
        Coupon coupon = getById(id);
        // 2. 有限定范围，需要删除优惠卷作用范围信息
        if (coupon.getSpecific()) {
            couponScopeService.removeByCouponId(id);
        }
        // 3. 删除优惠卷信息
        removeById(id);
    }
    
    /**
     * 查看优惠卷（根据IO查询优惠卷）
     */
    @Override
    public CouponDetailVO getCoupon(Long id) {
        // 1. 查询优惠卷
        Coupon coupon = getById(id);
        CouponDetailVO vo = BeanUtil.toBean(coupon, CouponDetailVO.class);
        // 2. 设置限定范围
        if (coupon.getSpecific()) {
            // 2.1. 查询限定范围列表
            List<CouponScope> scopeList = couponScopeService.getListByCouponId(id);
            // 2.2. 根据限定类型分组（1分类一组，2课程一组；取业务ID）
            Map<Integer, List<Long>> csMap = scopeList.stream()
                    .collect(Collectors.groupingBy(CouponScope::getType, Collectors.mapping(CouponScope::getBizId, Collectors.toList())));
            // 2.3. 查询分类名称
            Map<Long, String> bizMap = new HashMap<>(scopeList.size());
            List<Long> categoryIdList = csMap.get(1);
            if (CollUtils.isNotEmpty(categoryIdList)) {
                List<CategoryBasicDTO> categoryList = categoryCache.queryCategoryByIds(categoryIdList);
                for (CategoryBasicDTO category : categoryList) {
                    bizMap.put(category.getId(), category.getName());
                }
            }
            // 2.4. 查询课程名称
            List<Long> courseIdList = csMap.get(2);
            if (CollUtils.isNotEmpty(courseIdList)) {
                List<CourseSimpleInfoDTO> courseList = courseClient.getSimpleInfoList(courseIdList);
                for (CourseSimpleInfoDTO course : courseList) {
                    bizMap.put(course.getId(), course.getName());
                }
            }
            // 2.5. 封装限定范围列表
            List<CouponScopeVO> scopes = new ArrayList<>(scopeList.size());
            for (CouponScope scope : scopeList) {
                scopes.add(new CouponScopeVO(scope.getBizId(), bizMap.get(scope.getBizId())));
            }
            vo.setScopes(scopes);
        }
        return vo;
    }
    
    /**
     * 暂停发放
     */
    @Override
    @Transactional
    public void pauseCoupon(Long id) {
        // 1. 查询旧优惠券
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        // 2. 只有【未开始】和【进行中】的优惠卷可以被暂停
        CouponStatus status = coupon.getStatus();
        if (status != UN_ISSUE && status != ISSUING) {
            return;
        }
        // 3. 更新状态
        boolean success = lambdaUpdate()
                .set(Coupon::getStatus, PAUSE)
                .eq(Coupon::getId, id)
                .update();
        if (!success) {
            log.error("重复暂停优惠券");
        }
        // 4. 删除缓存
        redisTemplate.delete(RedisConstant.COUPON_CACHE_KEY_PREFIX + id);
    }
    
    /**
     * 查询发放中的优惠卷
     */
    @Override
    public List<CouponVO> queryIssuedCoupon() {
        // 1. 查询发放中并且是手动领取的优惠卷
        List<Coupon> list = lambdaQuery()
                .eq(Coupon::getStatus, ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .list();
        if (CollUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 2. 统计当前用户对优惠卷的领取情况
        // 2.1. 获取优惠卷id列表
        List<Long> couponIdList = list.stream().map(Coupon::getId).collect(Collectors.toList());
        // 2.2. 查询当前用户已经领取的优惠卷
        List<UserCoupon> userCouponList = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .in(UserCoupon::getCouponId, couponIdList)
                .list();
        // 2.3. 统计当前用户对优惠券的已经领取数量
        Map<Long, Long> issuedMap = userCouponList.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 2.4. 统计当前用户对优惠券的已经领取并且未使用的数量
        Map<Long, Long> userCouponMap = userCouponList.stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 3. 封装VO
        List<CouponVO> voList = new ArrayList<>(list.size());
        for (Coupon entity : list) {
            CouponVO vo = BeanUtils.copyBean(entity, CouponVO.class);
            voList.add(vo);
            // 3.1. 设置优惠卷【是否可以领取】
            // 是否剩余: 已领取数量 < 优惠卷总量
            boolean isRemain = entity.getIssueNum() < entity.getTotalNum();
            // 是否超额: 用户已领取数量 < 用户领取限制
            boolean isOver = issuedMap.getOrDefault(entity.getId(), 0L) < entity.getUserLimit();
            vo.setAvailable(isRemain && isOver);
            // 3.2. 设置优惠卷【是否可以使用】: 用户已经领取并且未使用的优惠券数量 > 0
            vo.setReceived(userCouponMap.getOrDefault(entity.getId(), 0L) > 0);
        }
        return voList;
    }
}
