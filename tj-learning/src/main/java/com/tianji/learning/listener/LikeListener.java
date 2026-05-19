package com.tianji.learning.listener;

import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-26
 * @Description: 点赞数量监听器
 */
@Component
@RequiredArgsConstructor
public class LikeListener {
    
    private final IInteractionReplyService replyService;
    
    /**
     * 点赞数量监听器
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.liked.times.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.QA_LIKED_TIMES_KEY
    ))
    public void updateLikeTimes(List<LikedTimesDTO> likedTimesDTOList) {
        List<InteractionReply> replyList = new ArrayList<>(likedTimesDTOList.size());
        likedTimesDTOList.forEach(likedTimesDTO -> {
            InteractionReply reply = new InteractionReply();
            reply.setId(likedTimesDTO.getBizId());
            reply.setLikedTimes(likedTimesDTO.getLikedTimes());
            replyList.add(reply);
        });
        replyService.updateBatchById(replyList);
    }
}
