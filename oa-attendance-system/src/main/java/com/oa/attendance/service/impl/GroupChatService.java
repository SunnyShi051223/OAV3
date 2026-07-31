package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.attendance.entity.*;
import com.oa.attendance.mapper.*;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupChatService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Autowired private ChatGroupMapper groupMapper;
    @Autowired private ChatGroupMemberMapper memberMapper;
    @Autowired private ChatMessageMapper messageMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private DataScopeService dataScopeService;
    @Autowired(required = false) private ChatWebSocketHandler webSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== 创建群聊 =====
    @Transactional
    public Result<ChatGroup> createGroup(String groupName, List<Long> memberIds) {
        SysUser current = dataScopeService.getCurrentUser();
        if (current == null) return Result.error("未登录");
        if (groupName == null || groupName.trim().isEmpty()) return Result.error("群名称不能为空");
        if (memberIds == null || memberIds.isEmpty()) return Result.error("请至少选择一个成员");

        ChatGroup group = new ChatGroup();
        group.setGroupName(groupName.trim());
        group.setCreatorId(current.getUserId());
        group.setCreateTime(LocalDateTime.now());
        groupMapper.insert(group);

        // 添加创建者
        addMember(group.getGroupId(), current.getUserId());
        // 添加其他成员（去重）
        for (Long uid : memberIds.stream().distinct().collect(Collectors.toList())) {
            if (!uid.equals(current.getUserId())) {
                addMember(group.getGroupId(), uid);
            }
        }

        // 发送系统消息
        String sysMsg = current.getRealName() + " 创建了群聊";
        sendGroupMessage(group.getGroupId(), NotificationService.ROBOT_SENDER_ID, sysMsg);

        group.setMembers(memberMapper.selectByGroupId(group.getGroupId()));
        return Result.success("创建成功", group);
    }

    // ===== 我的群聊列表 =====
    public Result<List<Map<String, Object>>> listMyGroups() {
        SysUser current = dataScopeService.getCurrentUser();
        if (current == null) return Result.error("未登录");

        List<ChatGroup> groups = groupMapper.selectByUserId(current.getUserId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatGroup g : groups) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("groupId", g.getGroupId());
            map.put("groupName", g.getGroupName());
            map.put("avatar", g.getAvatar());
            map.put("creatorId", g.getCreatorId());
            map.put("memberCount", memberMapper.selectByGroupId(g.getGroupId()).size());

            ChatMessage lastMsg = messageMapper.selectLastGroupMessage(g.getGroupId());
            if (lastMsg != null) {
                String sender = NotificationService.ROBOT_SENDER_ID.equals(lastMsg.getSenderId())
                        ? "系统" : (lastMsg.getSenderName() != null ? lastMsg.getSenderName() : "用户");
                map.put("lastMessage", sender + ": " + lastMsg.getContent());
                map.put("lastMessageTime", lastMsg.getCreateTime() != null
                        ? lastMsg.getCreateTime().format(DATETIME_FMT) : null);
            }
            map.put("unreadCount", messageMapper.countGroupUnread(g.getGroupId(), current.getUserId()));
            result.add(map);
        }
        return Result.success("查询成功", result);
    }

    // ===== 群聊消息 =====
    public Result<List<Map<String, Object>>> getGroupMessages(Long groupId) {
        SysUser current = dataScopeService.getCurrentUser();
        if (current == null) return Result.error("未登录");
        if (!isMember(groupId, current.getUserId())) return Result.error("您不是该群成员");

        messageMapper.markGroupRead(groupId);

        List<ChatMessage> messages = messageMapper.selectGroupMessages(groupId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage msg : messages) {
            result.add(toMsgMap(msg));
        }
        return Result.success("查询成功", result);
    }

    // ===== 发送群消息 =====
    @Transactional
    public Result<Map<String, Object>> sendGroupMessage(Long groupId, String content) {
        SysUser current = dataScopeService.getCurrentUser();
        if (current == null) return Result.error("未登录");
        if (!isMember(groupId, current.getUserId())) return Result.error("您不是该群成员");
        if (content == null || content.trim().isEmpty()) return Result.error("消息不能为空");

        ChatMessage msg = sendGroupMessage(groupId, current.getUserId(), content.trim());
        return Result.success("发送成功", toMsgMap(msg));
    }

    // ===== 群成员列表 =====
    public Result<List<ChatGroupMember>> getGroupMembers(Long groupId) {
        SysUser current = dataScopeService.getCurrentUser();
        if (current == null) return Result.error("未登录");
        if (!isMember(groupId, current.getUserId())) return Result.error("您不是该群成员");

        return Result.success("查询成功", memberMapper.selectByGroupId(groupId));
    }

    // ===== 添加成员 =====
    @Transactional
    public Result<?> addMembers(Long groupId, List<Long> userIds) {
        SysUser current = dataScopeService.getCurrentUser();
        if (current == null) return Result.error("未登录");
        if (!isMember(groupId, current.getUserId())) return Result.error("您不是该群成员");

        for (Long uid : userIds) {
            if (!isMember(groupId, uid)) {
                addMember(groupId, uid);
            }
        }
        return Result.success("添加成功");
    }

    // ===== 退出群聊 =====
    @Transactional
    public Result<?> leaveGroup(Long groupId) {
        SysUser current = dataScopeService.getCurrentUser();
        if (current == null) return Result.error("未登录");

        QueryWrapper<ChatGroupMember> w = new QueryWrapper<>();
        w.eq("group_id", groupId).eq("user_id", current.getUserId());
        memberMapper.delete(w);

        // 通知群聊
        String sysMsg = current.getRealName() + " 退出了群聊";
        sendGroupMessage(groupId, NotificationService.ROBOT_SENDER_ID, sysMsg);

        return Result.success("已退出群聊");
    }

    // ===== 内部方法 =====
    private void addMember(Long groupId, Long userId) {
        ChatGroupMember m = new ChatGroupMember();
        m.setGroupId(groupId);
        m.setUserId(userId);
        m.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(m);
    }

    private boolean isMember(Long groupId, Long userId) {
        QueryWrapper<ChatGroupMember> w = new QueryWrapper<>();
        w.eq("group_id", groupId).eq("user_id", userId);
        return memberMapper.selectCount(w) > 0;
    }

    private ChatMessage sendGroupMessage(Long groupId, Long senderId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(senderId);
        msg.setReceiverId(0L); // group marker
        msg.setGroupId(groupId);
        msg.setContent(content);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);

        // WebSocket 推送给所有群成员
        if (webSocketHandler != null) {
            try {
                List<Long> memberIds = memberMapper.selectMemberIdsExcept(groupId, senderId);
                Map<String, Object> wsMsg = new LinkedHashMap<>();
                wsMsg.put("type", "group_message");
                wsMsg.put("messageId", msg.getMessageId());
                wsMsg.put("groupId", groupId);
                wsMsg.put("senderId", senderId);
                wsMsg.put("senderName", NotificationService.ROBOT_SENDER_ID.equals(senderId)
                        ? NotificationService.ROBOT_NAME : resolveSenderName(senderId));
                wsMsg.put("content", content);
                wsMsg.put("createTime", msg.getCreateTime().toString());
                String json = objectMapper.writeValueAsString(wsMsg);
                for (Long uid : memberIds) {
                    webSocketHandler.pushToUser(uid, json);
                }
                // Also push to sender
                wsMsg.put("type", "group_message");
                webSocketHandler.pushToUser(senderId, json);
            } catch (Exception ignored) {}
        }
        return msg;
    }

    private String resolveSenderName(Long userId) {
        if (userId == null || userId == 0) return "系统";
        SysUser user = userMapper.selectById(userId);
        return user != null ? user.getRealName() : "用户";
    }

    private Map<String, Object> toMsgMap(ChatMessage msg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("messageId", msg.getMessageId());
        map.put("senderId", msg.getSenderId());
        map.put("senderName", NotificationService.ROBOT_SENDER_ID.equals(msg.getSenderId())
                ? NotificationService.ROBOT_NAME : (msg.getSenderName() != null ? msg.getSenderName() : resolveSenderName(msg.getSenderId())));
        map.put("groupId", msg.getGroupId());
        map.put("content", msg.getContent());
        map.put("createTime", msg.getCreateTime());
        return map;
    }
}