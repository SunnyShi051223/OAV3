package com.oa.attendance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.attendance.entity.ChatMessage;
import com.oa.attendance.mapper.ChatMessageMapper;
import com.oa.attendance.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 机器人通知服务 —— 以 senderId=0 的机器人身份向指定用户发送站内通知
 */
@Service
public class NotificationService {

    public static final Long ROBOT_SENDER_ID = 0L;
    public static final String ROBOT_NAME = "机器人通知";

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired(required = false)
    private ChatWebSocketHandler webSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 向指定用户发送机器人通知
     * @param userId 接收通知的用户ID
     * @param content 通知内容
     */
    public void sendToUser(Long userId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(ROBOT_SENDER_ID);
        msg.setReceiverId(userId);
        msg.setContent(content);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(msg);

        if (userId.equals(ROBOT_SENDER_ID)) return;

        if (webSocketHandler != null) {
            try {
                Map<String, Object> wsMsg = new HashMap<>();
                wsMsg.put("type", "message");
                wsMsg.put("messageId", msg.getMessageId());
                wsMsg.put("senderId", ROBOT_SENDER_ID);
                wsMsg.put("senderName", ROBOT_NAME);
                wsMsg.put("receiverId", userId);
                wsMsg.put("content", content);
                wsMsg.put("createTime", msg.getCreateTime().toString());
                webSocketHandler.pushToUser(userId, objectMapper.writeValueAsString(wsMsg));
            } catch (Exception ignored) {
            }
        }
    }
}
