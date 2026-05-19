package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikeRecordFormDTO;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.properties.BizTypesProperties;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-24
 */
@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 点赞或取消点赞（MySQL版）
     */
//    @Override
//    public void addLikeRecord(LikeRecordFormDTO recordDTO) {
//        Long userId = UserContext.getUser();
//
//        LikedRecord entity = lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .eq(LikedRecord::getBizId, recordDTO.getBizId())
//                .one();
//        // 1. 判断是否点赞
//        if (recordDTO.getLiked()) {
//            // 2. 判断是否点过赞
//            if (entity != null) {
//                return;
//            }
//            // 新增点赞记录
//            entity = BeanUtil.toBean(recordDTO, LikedRecord.class);
//            entity.setUserId(userId);
//            save(entity);
//        } else {
//            // 2. 判断是否点过赞
//            if (entity == null) {
//                return;
//            }
//            // 删除点赞记录
//            lambdaUpdate()
//                    .eq(LikedRecord::getId, entity.getId())
//                    .remove();
//        }
//        // 3. 统计点赞数
//        Integer count = lambdaQuery()
//                .eq(LikedRecord::getBizId, recordDTO.getBizId())
//                .count();
//        // 4. 发送更新点赞数量消息
//        rabbitTemplate.convertAndSend(
//                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
//                StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, recordDTO.getBizType()),
//                LikedTimesDTO.of(recordDTO.getBizId(), count)
//        );
//    }
    
    /**
     * 点赞或取消点赞（Redis版）
     */
    @Override
    public void addLikeRecord(LikeRecordFormDTO recordDTO) {
        Long userId = UserContext.getUser();
        // 1. 判断是否点赞 (Set)
        String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + recordDTO.getBizId();
        Long result;
        if (recordDTO.getLiked()) {
            // 添加点赞记录
            result = redisTemplate.opsForSet().add(key, userId.toString());
        } else {
            // 删除点赞记录
            result = redisTemplate.opsForSet().remove(key, userId.toString());
        }
        // TODO 添加或删除数据库点赞记录
        
        if (result == null || result == 0) {
            return;
        }
        // 2. 统计点赞数量
        Long count = redisTemplate.opsForSet().size(key);
        if (count == null) {
            return;
        }
        // 3. 缓存点赞数量 (SortedSet)
        String countKey = RedisConstants.LIKES_TIMES_KEY_PREFIX + recordDTO.getBizType();
        redisTemplate.opsForZSet().add(countKey, recordDTO.getBizId().toString(), count);
    }
    
    /**
     * 批量查询点赞状态（查询当前用户是否点赞了指定的业务）（MySQL版）
     */
//    @Override
//    public Set<Long> queryLikedStatus(Set<Long> bizIds) {
//        // select biz_id from liked_record where user_id = ? and biz_id in (?,?,?)
//        List<LikedRecord> list = lambdaQuery()
//                .select(LikedRecord::getBizId)
//                .eq(LikedRecord::getUserId, UserContext.getUser())
//                .in(LikedRecord::getBizId, bizIds)
//                .list();
//        // 将biz_id取出
//        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
//    }
    
    /**
     * 批量查询点赞状态（查询当前用户是否点赞了指定的业务）（Redis版）
     */
    @Override
    public Set<Long> queryLikedStatus(List<Long> bizIds) {
        Long userId = UserContext.getUser();

//        Set<Long> returnBizIds = new HashSet<>();
//        bizIds.forEach(bizId -> {
//            String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + bizId;
//            if (BooleanUtil.isTrue(redisTemplate.opsForSet().isMember(key, userId.toString()))) {
//                returnBizIds.add(bizId);
//            }
//        });
//        return returnBizIds;
        
        /*
            优化：Redis中提供了一个功能，可以在一次请求中执行多个命令，实现批处理效果：Pipeline
            注意：不要在一次批处理中传输太多命令，否则单次命令占用宽带过多，会导致网络阻塞
         */
        int BATCH_SIZE = 100;
        List<Object> allResults = new ArrayList<>();
        for (int i = 0; i < bizIds.size(); i += BATCH_SIZE) {
            int start = i;
            int end = Math.min(i + BATCH_SIZE, bizIds.size());
            
            List<Object> batchResults = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                StringRedisConnection src = (StringRedisConnection) connection;
                for (int j = start; j < end; j++) {
                    String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + bizIds.get(j);
                    src.sIsMember(key, userId.toString());
                }
                return null;
            });
            // TODO 缓存没有, 查询数据库
            
            allResults.addAll(batchResults);
        }
        return IntStream
                .range(0, allResults.size()) // 创建从0到集合size的流
                .filter(i -> (boolean) allResults.get(i)) // 遍历每个元素，保留结构为true的角标
                .mapToObj(bizIds::get) // 角标转成对应的业务id
                .collect(Collectors.toSet());
    }
    
    private final BizTypesProperties bizTypesProperties;
    private static final long MAX_BIZ_SIZE = 30;
    private final RabbitTemplate rabbitTemplate;
    
    @Override
    public void readLikedTimesAndSendMessage() {
        for (String bizType : bizTypesProperties.getBizTypes()) {
            // 1. 读取并移除点赞数量
            String key = RedisConstants.LIKES_TIMES_KEY_PREFIX + bizType;
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().popMin(key, MAX_BIZ_SIZE);
            if (CollUtils.isEmpty(tuples)) {
                continue;
            }
            // 2. 转换DTO
            List<LikedTimesDTO> list = new ArrayList<>(tuples.size());
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                String bizId = tuple.getValue();
                Double likedTimes = tuple.getScore();
                if (bizId == null || likedTimes == null) {
                    continue;
                }
                list.add(LikedTimesDTO.of(Long.valueOf(bizId), likedTimes.intValue()));
            }
            // 3. 发送MQ消息
            rabbitTemplate.convertAndSend(
                    MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
                    StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, bizType),
                    list
            );
        }
    }
    
    @Override
    public void persistenceLikedRecord() {
    
    }
    
}
