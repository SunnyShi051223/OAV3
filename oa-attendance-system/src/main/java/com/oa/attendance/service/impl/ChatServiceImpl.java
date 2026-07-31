package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.dto.ChatSendDTO;
import com.oa.attendance.entity.ChatMessage;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysRole;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.ChatMessageMapper;
import com.oa.attendance.mapper.SysRoleMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.service.IChatService;
import com.oa.attendance.vo.ChatContactVO;
import com.oa.attendance.vo.ChatMessageVO;
import com.oa.attendance.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements IChatService {

    private static final String ROLE_MANAGER = "MANAGER";
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired(required = false)
    private ChatWebSocketHandler webSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result<List<ChatContactVO>> getContacts() {
        SysUser current = dataScopeService.getCurrentUser();
        if (current == null) {
            return Result.error("未登录");
        }

        List<ChatContactVO> contacts = new ArrayList<>();

        // 机器人通知始终置顶
        ChatContactVO robot = new ChatContactVO();
        robot.setUserId(NotificationService.ROBOT_SENDER_ID);
        robot.setRealName(NotificationService.ROBOT_NAME);
        robot.setAvatar(null);
        robot.setDeptName("系统");
        robot.setPositionName("通知助手");
        robot.setRoleName("机器人");

        ChatMessage lastRobotMsg = getLastMessage(current.getUserId(), NotificationService.ROBOT_SENDER_ID);
        if (lastRobotMsg != null) {
            robot.setLastMessage(lastRobotMsg.getContent());
            robot.setLastMessageTime(lastRobotMsg.getCreateTime() != null
                    ? lastRobotMsg.getCreateTime().format(DATETIME_FMT) : null);
        }
        robot.setUnreadCount(countUnread(NotificationService.ROBOT_SENDER_ID, current.getUserId()));
        contacts.add(robot);

        // 查出有过聊天记录的联系人（排除机器人）
        List<SysUser> chatUsers = chatMessageMapper.selectChatContacts(current.getUserId());

        for (SysUser user : chatUsers) {
            ChatContactVO vo = new ChatContactVO();
            vo.setUserId(user.getUserId());
            vo.setRealName(user.getRealName());
            vo.setAvatar(user.getAvatar());
            vo.setDeptName(user.getDeptName());
            vo.setPositionName(user.getPositionName());
            vo.setRoleName(user.getRoleName());

            ChatMessage lastMsg = getLastMessage(current.getUserId(), user.getUserId());
            if (lastMsg != null) {
                vo.setLastMessage(lastMsg.getContent());
                vo.setLastMessageTime(lastMsg.getCreateTime() != null
                        ? lastMsg.getCreateTime().format(DATETIME_FMT) : null);
            }
            vo.setUnreadCount(countUnread(user.getUserId(), current.getUserId()));

            contacts.add(vo);
        }

        // 机器人保持第一位，其余按最新消息时间排序
        List<ChatContactVO> nonRobot = new ArrayList<>(contacts.subList(1, contacts.size()));
        nonRobot.sort((a, b) -> {
            if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        List<ChatContactVO> result = new ArrayList<>();
        result.add(contacts.get(0)); // robot first
        result.addAll(nonRobot);
        return Result.success("查询成功", result);
    }

    @Override
    public Result<List<ChatMessageVO>> getMessages(Long contactId) {
        SysUser current = dataScopeService.getCurrentUser();
        if (current == null) {
            return Result.error("未登录");
        }

        // 标记已读
        chatMessageMapper.markAsRead(current.getUserId(), contactId);

        List<ChatMessage> messages = chatMessageMapper.selectConversation(
                current.getUserId(), contactId);
        List<ChatMessageVO> vos = messages.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return Result.success("查询成功", vos);
    }

    @Override
    @Transactional
    public Result<ChatMessageVO> send(ChatSendDTO dto) {
        SysUser current = dataScopeService.getCurrentUser();
        if (current == null) {
            return Result.error("未登录");
        }

        if (current.getUserId().equals(dto.getReceiverId())) {
            return Result.error("不能给自己发消息");
        }

        if (dto.getContent().length() > 2000) {
            return Result.error("消息内容不能超过2000字");
        }

        SysUser receiver = sysUserMapper.selectById(dto.getReceiverId());
        if (receiver == null || Integer.valueOf(1).equals(receiver.getDeleted())
                || Integer.valueOf(0).equals(receiver.getStatus())) {
            return Result.error("接收人不存在或已禁用");
        }

        // 检查是否有权限与该用户聊天
        Map<Long, String> roleCodeMap = buildRoleCodeMap(
                java.util.Collections.singletonList(receiver));
        if (!canChat(current, receiver, roleCodeMap)) {
            return Result.error("无法与该用户聊天");
        }

        ChatMessage message = new ChatMessage();
        message.setSenderId(current.getUserId());
        message.setReceiverId(dto.getReceiverId());
        message.setContent(dto.getContent());
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);

        ChatMessageVO vo = new ChatMessageVO();
        vo.setMessageId(message.getMessageId());
        vo.setSenderId(current.getUserId());
        vo.setSenderName(current.getRealName());
        vo.setSenderAvatar(current.getAvatar());
        vo.setReceiverId(dto.getReceiverId());
        vo.setContent(dto.getContent());
        vo.setIsRead(0);
        vo.setCreateTime(message.getCreateTime());

        // 通过 WebSocket 实时推送给接收方
        if (webSocketHandler != null) {
            try {
                Map<String, Object> wsMsg = new HashMap<>();
                wsMsg.put("type", "message");
                wsMsg.put("messageId", message.getMessageId());
                wsMsg.put("senderId", current.getUserId());
                wsMsg.put("senderName", current.getRealName());
                wsMsg.put("receiverId", dto.getReceiverId());
                wsMsg.put("content", dto.getContent());
                wsMsg.put("createTime", message.getCreateTime().toString());
                webSocketHandler.pushToUser(dto.getReceiverId(), objectMapper.writeValueAsString(wsMsg));
            } catch (Exception ignored) {
            }
        }

        return Result.success("发送成功", vo);
    }

    private boolean canChat(SysUser currentUser, SysUser target, Map<Long, String> roleCodeMap) {
        return true;
    }

    private Map<Long, String> buildRoleCodeMap(List<SysUser> users) {
        return users.stream().collect(Collectors.toMap(
                SysUser::getUserId,
                user -> user.getRoleCode() != null ? user.getRoleCode()
                        : resolveRoleCode(user.getRoleId()),
                (a, b) -> a));
    }

    private String resolveRoleCode(Long roleId) {
        if (roleId == null) return null;
        SysRole role = sysRoleMapper.selectById(roleId);
        return role == null ? null : role.getRoleCode();
    }

    private ChatMessage getLastMessage(Long userId1, Long userId2) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w
                        .and(a -> a.eq("sender_id", userId1).eq("receiver_id", userId2))
                        .or(a -> a.eq("sender_id", userId2).eq("receiver_id", userId1)))
                .orderByDesc("create_time")
                .last("LIMIT 1");
        return chatMessageMapper.selectOne(wrapper);
    }

    private int countUnread(Long senderId, Long receiverId) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("sender_id", senderId)
                .eq("receiver_id", receiverId)
                .eq("is_read", 0);
        Long count = chatMessageMapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    private ChatMessageVO toVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setMessageId(message.getMessageId());
        vo.setSenderId(message.getSenderId());
        vo.setSenderName(NotificationService.ROBOT_SENDER_ID.equals(message.getSenderId())
                ? NotificationService.ROBOT_NAME : message.getSenderName());
        vo.setSenderAvatar(message.getSenderAvatar());
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
