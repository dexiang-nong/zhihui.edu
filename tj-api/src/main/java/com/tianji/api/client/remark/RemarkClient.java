package com.tianji.api.client.remark;

import com.tianji.api.client.remark.fallback.RemarkClientFallback;
import com.tianji.api.dto.remark.LikeRecordFormDTO;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-24
 * @Description: Remark的Feign接口
 */
@FeignClient(value = "remark-service", fallbackFactory = RemarkClientFallback.class)
public interface RemarkClient {
    
    /**
     * 批量查询点赞状态（查询当前用户是否点赞了指定的业务）
     */
    @GetMapping("/likes/list")
    Set<Long> queryLikedStatus(@RequestParam("bizIds") List<Long> bizIds);
    
    /**
     * 点赞或取消点赞
     */
    @PostMapping("/likes")
    void addLikeRecord(@Valid @RequestBody LikeRecordFormDTO recordDTO);
    
}
