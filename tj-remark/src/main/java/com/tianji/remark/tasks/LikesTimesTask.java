package com.tianji.remark.tasks;

import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-26
 * @Description: 点赞数推送定时器
 */
@Component
@RequiredArgsConstructor
public class LikesTimesTask {
    
    private final ILikedRecordService likedRecordService;
    
    /**
     * 推送点赞数量更新, 每30秒执行一次
     */
    @Scheduled(fixedDelay = 30000)
    @Scheduled(cron = "0/30 * * * * ?")
    public void pushLikesTimes() {
        likedRecordService.readLikedTimesAndSendMessage();
    }
    
    /**
     * 推送点赞数量更新, 每30秒执行一次
     */
//    @XxlJob("pushLikesTimesJob")
//    public void pushLikesTimes() {
//        likedRecordService.readLikedTimesAndSendMessage();
//    }
}
