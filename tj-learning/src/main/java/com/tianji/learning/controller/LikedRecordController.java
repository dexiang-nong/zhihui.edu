package com.tianji.learning.controller;

import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.dto.remark.LearningLikeRecordFromDTO;
import com.tianji.api.dto.remark.LikeRecordFormDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * <p>
 * 点赞系统相关接口
 * </p>
 *
 * @author 張德帥
 * @version 1.0
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "点赞系统相关接口")
@RequestMapping("/likes")
public class LikedRecordController {
    
    private final RemarkClient remarkClient;
    
    /**
     * 点赞或取消点赞
     */
    @Operation(summary = "点赞或取消点赞")
    @PostMapping
    public void addLikeRecord(@Valid @RequestBody LearningLikeRecordFromDTO learningLikeRecordFromDTO) {
        // TODO 前端不传送业务类型，则使用默认值
        LikeRecordFormDTO likeRecordFormDTO = new LikeRecordFormDTO();
        likeRecordFormDTO.setBizId(learningLikeRecordFromDTO.getId());
        likeRecordFormDTO.setBizType("QA");
        likeRecordFormDTO.setLiked(learningLikeRecordFromDTO.getLiked());
        remarkClient.addLikeRecord(likeRecordFormDTO);
    }
}
