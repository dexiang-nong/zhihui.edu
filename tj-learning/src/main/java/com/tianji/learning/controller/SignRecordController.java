package com.tianji.learning.controller;

import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-28
 * @Description: 签到接口管理
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "签到接口管理")
public class SignRecordController {
    
    private final ISignRecordService signRecordService;
    
    /**
     * 签到
     */
    @Operation(summary = "签到")
    @PostMapping("/sign-records")
    public SignResultVO signRecords() {
        return signRecordService.signRecords();
    }
    
    /**
     * 查询本月签到记录
     */
    @Operation(summary = "查询本月签到记录")
    @GetMapping("/sign-records")
    public List<String> querySignRecord() {
        return signRecordService.querySignRecord();
    }
}
