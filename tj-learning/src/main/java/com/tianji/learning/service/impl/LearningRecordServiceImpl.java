package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.enums.LessonStatus;
import com.tianji.learning.domain.enums.SectionType;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-18
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {
    
    private final ILearningLessonService learningLessonService;
    
    private final CourseClient courseClient;
    
    private final LearningRecordDelayTaskHandler delayTaskHandler;
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 查询学习记录
     */
    @Override
    public LearningLessonDTO queryLearningRecord(Long courseId) {
        // 1. 查询课表信息
        LearningLesson lesson = learningLessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, UserContext.getUser())
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        // 2. 查询学习记录
        List<LearningRecord> recordList = lambdaQuery()
                .eq(LearningRecord::getLessonId, lesson.getId())
                .list();
        // 3. 封装结果
        LearningLessonDTO lessonDTO = new LearningLessonDTO();
        lessonDTO.setId(lesson.getId());
        lessonDTO.setLatestSectionId(lesson.getLatestSectionId());
        lessonDTO.setRecords(BeanUtil.copyToList(recordList, LearningRecordDTO.class));
        return lessonDTO;
    }
    
    /**
     * 提交学习记录   <br>
     * 接口描述
     * <ul>
     *     <li>视频：当前播放进度超过50%则判定本节学完</li>
     *     <li>考试：考试结束时提交记录，直接判定为本节学完</li>
     * </ul>
     */
    @Override
    @Transactional
    public void submitLearningRecord(LearningRecordFormDTO recordDTO) {
        // 1. 获取登录用户
        Long userId = UserContext.getUser();
        // 2. 处理学习记录
        boolean finished;
        if (recordDTO.getSectionType() == SectionType.VIDEO) {
            // 2.1. 处理视频
            finished = handleVideoRecord(userId, recordDTO);
        } else {
            // 2.2. 处理考试
            finished = handleExamRecord(userId, recordDTO);
        }
        
        if (!finished) {
            // 没有新学完的小节，无需更新课表中的学习进度
            return;
        }
        // 3. 处理课表数据
        handleLearningLessonsChanges(recordDTO);
    }
    
    /**
     * 处理课表数据
     */
    private void handleLearningLessonsChanges(LearningRecordFormDTO recordDTO) {
        // 1. 查询课表
        LearningLesson lesson = learningLessonService.getById(recordDTO.getLessonId());
        if (lesson == null) {
            throw new BizIllegalException("课程不存在，无法更新数据！");
        }
        // 2. 判断是否有新的完成小节
        boolean allLearned;
        // 3. 如果有新完成的小节，则需要查询课程数据
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cInfo == null) {
            throw new BizIllegalException("课程不存在，无法更新数据！");
        }
        // 4. 比较课程是否全部学完：已学习小节 >= 课程总小节
        Integer learnedSections = lesson.getLearnedSections();
        allLearned = learnedSections + 1 >= cInfo.getSectionNum();
        // 5. 更新课表
        learningLessonService.lambdaUpdate()
                .set(learnedSections == 0, LearningLesson::getStatus, LessonStatus.LEARNING)
                .set(allLearned, LearningLesson::getStatus, LessonStatus.FINISHED)
                .set(allLearned, LearningLesson::getLatestLearnTime, LocalDateTime.now())
                .setSql("learned_sections = learned_sections + 1")
                .eq(LearningLesson::getId, lesson.getId())
                .update();
        // 6. 添加学习积分
        rabbitTemplate.convertAndSend(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.LEARN_SECTION,
                UserContext.getUser()
        );
    }
    
    /**
     * 处理视频
     */
    private boolean handleVideoRecord(Long userId, LearningRecordFormDTO recordDTO) {
        // 1. 查询旧的学习记录
        LearningRecord old = queryOldRecord(recordDTO.getLessonId(), recordDTO.getSectionId());
        // 2. 判断是否存在
        if (old == null) {
            // 3. 不存在，则新增
            // 3.1. 转换PO
            LearningRecord record = BeanUtils.copyBean(recordDTO, LearningRecord.class);
            // 3.2. 填充数据
            record.setUserId(userId);
            // 3.3. 写入数据库
            boolean success = save(record);
            if (!success) {
                throw new DbException("新增学习记录失败！");
            }
            return false;
        }
        // 4. 存在，则更新
        // 4.1. 判断是否是第一次完成
        boolean finished = !old.getFinished() && recordDTO.getMoment() * 2 >= recordDTO.getDuration();
        if (!finished) {
            LearningRecord record = new LearningRecord();
            record.setLessonId(recordDTO.getLessonId());
            record.setSectionId(recordDTO.getSectionId());
            record.setMoment(recordDTO.getMoment());
            record.setId(old.getId());
            record.setFinished(old.getFinished());
            delayTaskHandler.addLearningRecordTask(record);
            return false;
        }
        // 4.2. 更新数据
        boolean success = lambdaUpdate()
                .set(LearningRecord::getMoment, recordDTO.getMoment())
                .set(LearningRecord::getFinished, true)
                .set(LearningRecord::getFinishTime, recordDTO.getCommitTime())
                .eq(LearningRecord::getId, old.getId())
                .update();
        if (!success) {
            throw new DbException("更新学习记录失败！");
        }
        // 4.3. 清理缓存
        delayTaskHandler.cleanRecordCache(recordDTO.getLessonId(), recordDTO.getSectionId());
        return true;
    }
    
    /**
     * 查询旧的学习记录
     */
    private LearningRecord queryOldRecord(Long lessonId, Long sectionId) {
        // 1. 查询缓存
        LearningRecord old = delayTaskHandler.readRecordCache(lessonId, sectionId);
        if (old != null) {
            return old;
        }
        // 2. 查询数据库
        old = lambdaQuery()
                .eq(LearningRecord::getLessonId, lessonId)
                .eq(LearningRecord::getSectionId, sectionId)
                .one();
        // 3. 写入缓存
        if (old != null) {
            delayTaskHandler.writeRecordCache(old);
        }
        
        return old;
    }
    
    /**
     * 处理考试
     */
    private boolean handleExamRecord(Long userId, LearningRecordFormDTO recordDTO) {
        // 1. 转换DTO为PO
        LearningRecord record = BeanUtils.copyBean(recordDTO, LearningRecord.class);
        // 2. 填充数据
        record.setUserId(userId);
        record.setFinished(true);
        record.setFinishTime(recordDTO.getCommitTime());
        // 3. 写入数据库
        boolean success = save(record);
        if (!success) {
            throw new DbException("新增考试记录失败！");
        }
        return true;
    }
}
