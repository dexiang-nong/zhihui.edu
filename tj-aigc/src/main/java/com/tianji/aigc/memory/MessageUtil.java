package com.tianji.aigc.memory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.tianji.aigc.constants.ToolConstant;
import com.tianji.aigc.tools.config.ToolResultHolder;
import org.springframework.ai.chat.messages.*;

import java.util.Map;

public class MessageUtil {

    public static String toJson(Message message) {
        MyMessage myMessage = BeanUtil.toBean(message, MyMessage.class);
        myMessage.setTextContent(message.getText());
        if (message instanceof AssistantMessage assistantMessage) {
            myMessage.setToolCalls(assistantMessage.getToolCalls());
            
            // 解析params参数, 存储到数据库中
            String messageId = Convert.toStr(assistantMessage.getMetadata().get(ToolConstant.ID));
            String requestId = Convert.toStr(ToolResultHolder.get(messageId, ToolConstant.REQUEST_ID));
            Map<String, Object> params = ToolResultHolder.get(requestId);
            if (ObjectUtil.isNotEmpty(params)) {
                myMessage.setParams(params);
            }
            ToolResultHolder.remove(messageId);
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            myMessage.setToolResponses(toolResponseMessage.getResponses());
        }
        return JSONUtil.toJsonStr(myMessage);
    }

    public static Message toMessage(String json) {
        MyMessage myMessage = JSONUtil.toBean(json, MyMessage.class);
        MessageType messageType = MessageType.valueOf(myMessage.getMessageType());
        // 消息类型的构建参考它的构造函数
        switch (messageType) {
            case SYSTEM -> {
                return SystemMessage.builder()
                        .text(myMessage.getTextContent())
                        .metadata(myMessage.getMetadata())
                        .build();
            }
            case USER -> {
                return UserMessage.builder()
                        .text(myMessage.getTextContent())
                        .media(myMessage.getMedia())
                        .metadata(myMessage.getMetadata())
                        .build();
            }
            case ASSISTANT -> {
                /*
                    AssistantMessage不能设置params参数
                    MyAssistantMessage extends AssistantMessage
                 */
                return new MyAssistantMessage(
                        myMessage.getTextContent(),
                        myMessage.getMetadata(),
                        myMessage.getToolCalls(),
                        myMessage.getMedia(),
                        myMessage.getParams()
                );
            }
            case TOOL -> {
                return ToolResponseMessage.builder()
                        .responses(myMessage.getToolResponses())
                        .metadata(myMessage.getMetadata())
                        .build();
            }
        }
        throw new RuntimeException("Message data conversion failed.");
    }

}
