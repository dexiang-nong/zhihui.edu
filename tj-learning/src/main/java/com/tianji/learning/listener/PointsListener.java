package com.tianji.learning.listener;

import com.tianji.common.constants.MqConstants;
import com.tianji.learning.domain.enums.PointsRecordType;
import com.tianji.learning.listener.message.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-28
 * @Description: 学习积分监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsListener {
    
    private final IPointsRecordService pointsRecordService;
    
    /**
     * 监听签到积分
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "sign.points.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.SIGN_IN
    ))
    public void listenSignInPoints(SignInMessage message) {
        pointsRecordService.addPointsRecord(message.getUserId(), message.getPoints(), PointsRecordType.SIGN);
    }
    
    /**
     * 监听课程学习积分
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.points.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.LEARN_SECTION
    ))
    public void listenVideoPoints(Long userId) {
        pointsRecordService.addPointsRecord(userId, 10, PointsRecordType.LEARNING);
    }
    
    /**
     * 监听回答积分
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "qa.points.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_REPLY
    ))
    public void listenReplyPoints(Long userId) {
        pointsRecordService.addPointsRecord(userId, 5, PointsRecordType.QA);
    }
    
    /**
     * 监听上传笔记积分
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "qa.points.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_NOTE
    ))
    public void listenSaveNotePoints(Long userId) {
        pointsRecordService.addPointsRecord(userId, 5, PointsRecordType.NOTE);
    }
    
    /**
     * 监听采集笔记积分
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "qa.points.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.NOTE_GATHERED
    ))
    public void listenGatherNotePoints(Long userId) {
        pointsRecordService.addPointsRecord(userId, 5, PointsRecordType.NOTE);
    }
}
