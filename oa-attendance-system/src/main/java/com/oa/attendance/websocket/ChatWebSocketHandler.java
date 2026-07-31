package com.oa.attendance.websocket;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.attendance.entity.ChatMessage;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.ChatMessageMapper;
import com.oa.attendance.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            sessions.put(userId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        Long senderId = getUserId(session);
        if (senderId == null) return;

        Map<String, Object> payload = objectMapper.readValue(textMessage.getPayload(), Map.class);
        String type = (String) payload.getOrDefault("type", "message");

        if ("message".equals(type)) {
            Long receiverId = toLong(payload.get("receiverId"));
            String content = (String) payload.get("content");
            if (receiverId == null || content == null || content.trim().isEmpty()) return;

            SysUser sender = userMapper.getUserDetailById(senderId);
            String senderName = sender != null ? sender.getRealName() : String.valueOf(senderId);

            ChatMessage msg = new ChatMessage();
            msg.setSenderId(senderId);
            msg.setReceiverId(receiverId);
            msg.setContent(content.trim());
            msg.setIsRead(0);
            msg.setCreateTime(LocalDateTime.now());
            chatMessageMapper.insert(msg);

            Map<String, Object> result = objectMapper.convertValue(msg, Map.class);
            result.put("senderName", senderName);
            result.put("type", "message");
            String json = objectMapper.writeValueAsString(result);

            sendToUser(receiverId, json);
            sendToUser(senderId, json);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            sessions.remove(userId);
        }
    }

    public void pushToUser(Long userId, String message) {
        sendToUser(userId, message);
    }

    private void sendToUser(Long userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException ignored) {
            }
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object val = session.getAttributes().get("userId");
        return val instanceof Long ? (Long) val : null;
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try { return Long.parseLong((String) value); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
