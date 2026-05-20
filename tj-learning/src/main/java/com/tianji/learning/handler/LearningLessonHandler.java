package com.tianji.learning.handler;

import com.tianji.common.utils.CollUtils;
import com.tianji.learning.domain.enums.LessonStatus;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-20
 * @Description: 课程过期检测
 */
@Component
@RequiredArgsConstructor
public class LearningLessonHandler {
    
    private final ILearningLessonService learningLessonService;
    
    /**
     * 每1分钟检查课程是否过期，如果已经过期，则设置为过期状态
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void checkExpired() {
        // 1. 查询状态未失效, 但失效时间已到的课表
        List<LearningLesson> list = learningLessonService.lambdaQuery()
                .select(LearningLesson::getId)
                .ne(LearningLesson::getStatus, LessonStatus.EXPIRED)
                .lt(LearningLesson::getExpireTime, LocalDateTime.now())
                .list();
        if (CollUtils.isEmpty(list)) {
            return;
        }
        // 2. 设置状态过期
        List<Long> ids = list.stream()
                .map(LearningLesson::getId)
                .collect(Collectors.toList());
        learningLessonService.lambdaUpdate()
                .set(LearningLesson::getStatus, LessonStatus.EXPIRED)
                .in(LearningLesson::getId, ids)
                .update();
    }
}
