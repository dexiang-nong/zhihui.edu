package com.tianji.aigc.tools;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.constants.ToolConstant;
import com.tianji.aigc.tools.config.ToolResultHolder;
import com.tianji.aigc.tools.result.CourseInfo;
import com.tianji.api.client.course.CourseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <p>
 * 课程Tool call
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-24
 */
@Component
@RequiredArgsConstructor
public class CourseTool {
    
    private final CourseClient courseClient;
    
    private static final String FIELD_NAME_FORMAT = "{}_{}";  // 提取格式字符串常量
    
    @Tool(description = "根据课程id查询课程详细信息")
    public CourseInfo queryCourseById(
            @ToolParam(description = "课程id") Long courseId,
            ToolContext toolContext // chatClient传递的工具上下文参数
    ) {
        return Optional.ofNullable(courseId)
                .map(id -> CourseInfo.of(courseClient.baseInfo(id, true)))
                .map(courseInfo -> {
                    // 字段: 类名 courseInfo
                    String field = StrUtil.format(FIELD_NAME_FORMAT,
                            StrUtil.lowerFirst(CourseInfo.class.getSimpleName()),
                            courseInfo.getId());
                    // 请求id: 从工具上下文获取
                    String requestId = Convert.toStr(toolContext.getContext().get(ToolConstant.REQUEST_ID));
                    // 存储到保持器中
                    ToolResultHolder.put(requestId, field, courseInfo);
                    
                    return courseInfo;
                })
                .orElse(null);
    }
    
    /*
        {
            "code": 200,
            "msg": "OK",
            "data": [
                {
                    "type": "USER",
                    "content": ""
                },
                {
                    "type": "ASSISTANT",
                    "content": "",
                    "params": {
                        "courseInfo_#{课程id}": {
                            "id": "",
                            "name": "",
                            "price": ,
                            "validDuration": ,
                            "usePeople": "",
                            "detail": ""
                        }
                    }
                }
            ],
            "requestId": ""
        }
     */
    
}
