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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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

    @Override
    public Result<List<ChatContactVO>> getContacts() {
        SysUser current = dataScopeService.getCurrentUser();
        if (current == null) {
            return Result.error("未登录");
        }

        List<SysUser> allUsers = sysUserMapper.selectAllActiveWithDetails();
        List<SysUser> users = allUsers.stream()
                .filter(u -> !u.getUserId().equals(current.getUserId()))
                .collect(Collectors.toList());

        // 加载所有角色信息，用于 MANAGER 过滤
        Map<Long, String> roleCodeMap = buildRoleCodeMap(users);

        List<SysUser> visibleUsers = users.stream()
                .filter(user -> canChat(current, user, roleCodeMap))
                .collect(Collectors.toList());

        // 获取每个联系人的最后一条消息和未读数
        List<ChatContactVO> contacts = new ArrayList<>();
        for (SysUser user : visibleUsers) {
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

        // 按最新消息时间排序
        contacts.sort((a, b) -> {
            if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        return Result.success("查询成功", contacts);
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

        return Result.success("发送成功", vo);
    }

    private boolean canChat(SysUser currentUser, SysUser target, Map<Long, String> roleCodeMap) {
        if (dataScopeService.hasFullDataAccess()) {
            return true;
        }

        if (dataScopeService.hasDepartmentDataAccess()) {
            Long myDeptId = currentUser.getDeptId();
            if (myDeptId != null && myDeptId.equals(target.getDeptId())) {
                return true;
            }
            String targetRoleCode = roleCodeMap.getOrDefault(target.getUserId(),
                    resolveRoleCode(target.getRoleId()));
            return ROLE_MANAGER.equals(targetRoleCode);
        }

        Long myDeptId = currentUser.getDeptId();
        return myDeptId != null && myDeptId.equals(target.getDeptId());
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
        vo.setSenderName(message.getSenderName());
        vo.setSenderAvatar(message.getSenderAvatar());
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
