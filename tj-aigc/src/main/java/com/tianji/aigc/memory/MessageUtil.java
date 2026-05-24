package com.tianji.aigc.memory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.chat.messages.*;

public class MessageUtil {

    public static String toJson(Message message) {
        MyMessage myMessage = BeanUtil.toBean(message, MyMessage.class);
        myMessage.setTextContent(message.getText());
        if (message instanceof AssistantMessage assistantMessage) {
            myMessage.setToolCalls(assistantMessage.getToolCalls());
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            myMessage.setToolResponses(toolResponseMessage.getResponses());
        }
        return JSONUtil.toJsonStr(myMessage);
    }

    public static Message toMessage(String json) {
        MyMessage myMessage = JSONUtil.toBean(json, MyMessage.class);
        MessageType messageType = MessageType.valueOf(myMessage.getMessageType());
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
                        .metadata(myMessage.getMetadata())
                        .build();
            }
            case ASSISTANT -> {
                return AssistantMessage.builder()
                        .content(myMessage.getTextContent())
                        .properties(myMessage.getMetadata())
                        .build();
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
