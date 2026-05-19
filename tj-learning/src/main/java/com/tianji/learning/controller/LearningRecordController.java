package com.tianji.learning.controller;


import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-18
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "学习记录管理相关接口")
@RequestMapping("/learning-records")
public class LearningRecordController {
    
    private final ILearningRecordService learningRecordService;
    
    /**
     * 查询学习记录
     */
    @Operation(summary = "查询学习记录")
    @GetMapping("/course/{courseId}")
    public LearningLessonDTO queryLearningRecord(@PathVariable Long courseId) {
        return learningRecordService.queryLearningRecord(courseId);
    }
    
    /**
     * 提交学习记录   <br>
     * 接口描述
     *  <ul>
     *      <li>视频：当前播放进度超过50%则判定本节学完</li>
     *      <li>考试：考试结束时提交记录，直接判定为本节学完</li>
     *  </ul>
     */
    @Operation(summary = "提交学习记录")
    @PostMapping
    public void submitLearningRecord(@RequestBody LearningRecordFormDTO recordFormDTO) {
        learningRecordService.submitLearningRecord(recordFormDTO);
    }

}
