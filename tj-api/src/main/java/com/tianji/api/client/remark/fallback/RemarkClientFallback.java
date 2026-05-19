package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.dto.remark.LikeRecordFormDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Set;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-24
 * @Description: Remark服务的fallback逻辑
 */
@Slf4j
public class RemarkClientFallback implements FallbackFactory<RemarkClient> {
    
    @Override
    public RemarkClient create(Throwable cause) {
        log.error("请求remark-service服务异常", cause);
        return new RemarkClient() {
            @Override
            public Set<Long> queryLikedStatus(List<Long> bizIds) {
                return Set.of();
            }
            
            @Override
            public void addLikeRecord(LikeRecordFormDTO recordDTO) {
                log.warn("添加点赞记录失败，请稍后再试");
            }
        };
    }
    
}
