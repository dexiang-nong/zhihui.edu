package com.tianji.learning.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.listener.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-28
 * @Description: 签到Service实现
 */
@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {
    
    private final StringRedisTemplate redisTemplate;
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 签到
     */
    @Override
    public SignResultVO signRecords() {
        // 1. 签到
        // 1.1. 构建KEY ( sign:uid:{user_id}:{yyyyMM} )
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        String format = now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId + format;
        // 1.2. 保存签到信息
        int dayOfMonth = now.getDayOfMonth(); // 这个月的第几天
        int offset = dayOfMonth - 1;
        // bitmap成功设置, 返回0->false
        Boolean isSuccess = redisTemplate.opsForValue().setBit(key, offset, true);
        if (BooleanUtil.isTrue(isSuccess)) {
            throw new BizIllegalException("不允许重复签到！");
        }
        // 2. 计算连续签到天数
        int signDays = countSignDays(key, dayOfMonth);
        // 3. 计算签到得分
        int rewardPoints = 0;
        switch (signDays) {
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }
        // 4. 保存积分记录
        rabbitTemplate.convertAndSend(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId, rewardPoints + 1) // 签到积分+奖励积分
        );
        // 5. 构建VO返回
        return SignResultVO.builder()
                .signDays(signDays)
                .rewardPoints(rewardPoints)
                .build();
    }
    
    /**
     * 查询本月签到记录
     */
    @Override
    public List<String> querySignRecord() {
        // 1. 获取本月从第一天开始，到今天为止的所有签到记录
        // 1.1. 构建key
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        int dayOfMonth = now.getDayOfMonth();
        String format = now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId + format;
        // 1.2. 获取签到记录
        List<Long> results = redisTemplate.opsForValue()
                .bitField(
                        key,
                        BitFieldSubCommands.create()
                                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                                .valueAt(0)
                );
        if (CollUtils.isEmpty(results)) {
            return List.of();
        }
        // 2. 将查询结果转换为2禁止
        Long num = results.get(0);
        if (num == null || num == 0) {
            return List.of();
        }
        String binary = Long.toBinaryString(num);
        // 3. 低位补0 (转换2进制, 会去除前置0)
        if (binary.length() < dayOfMonth) {
            binary = "0".repeat(dayOfMonth - binary.length()) + binary;
        }
        return CollUtils.toList(binary.split(""));
    }
    
    /**
     * 统计连续签到天数
     */
    public int countSignDays(String key, int dayOfMonth) {
        // 1. 获取本月从第一天开始，到今天为止的所有签到记录
        List<Long> results = redisTemplate.opsForValue()
                .bitField(
                        key,
                        BitFieldSubCommands.create()
                                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                                .valueAt(0)
                );
        if (CollUtils.isEmpty(results)) {
            return 0;
        }
        // 2. 取出十进制结果
        Long num = results.get(0);
        if (num == null || num == 0) {
            return 0;
        }
        // 3. 统计连续签到天数
        int signDays = 0;
        // 让num与1作与运算，得到数字的最后一个bit位，如果不为0，则进入循环体
        while ((num & 1) == 1) {
            // 为1, 说明签到, 签到天数+1
            signDays++;
            // 右移1位, 抛弃最后一个bit
            num >>>= 1;
        }
        return signDays;
        
        /*
            假设BitMap：0 1 1，执行流程如下
                第一次循环：
                      0 1 1
                    & 0 0 1 => 1，signCount = 1
                第二次循环：
                      0 1
                    & 0 1 => 1，signCount = 2
                第三次循环：
                      0
                    & 1 => 0，结束循环
         */
    }
}
