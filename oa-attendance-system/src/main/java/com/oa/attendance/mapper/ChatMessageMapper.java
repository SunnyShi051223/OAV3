package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT m.*, u.real_name AS sender_name, u.avatar AS sender_avatar " +
            "FROM chat_message m " +
            "LEFT JOIN sys_user u ON m.sender_id = u.user_id " +
            "WHERE ((m.sender_id = #{userId} AND m.receiver_id = #{contactId}) " +
            "   OR (m.sender_id = #{contactId} AND m.receiver_id = #{userId})) " +
            "ORDER BY m.create_time ASC")
    List<ChatMessage> selectConversation(@Param("userId") Long userId,
                                          @Param("contactId") Long contactId);

    @Select("SELECT m.*, u.real_name AS sender_name, u.avatar AS sender_avatar " +
            "FROM chat_message m " +
            "LEFT JOIN sys_user u ON m.sender_id = u.user_id " +
            "WHERE m.receiver_id = #{userId} AND m.is_read = 0 " +
            "ORDER BY m.create_time DESC")
    List<ChatMessage> selectUnreadMessages(@Param("userId") Long userId);

    @Update("UPDATE chat_message SET is_read = 1 " +
            "WHERE receiver_id = #{receiverId} AND sender_id = #{senderId} AND is_read = 0")
    int markAsRead(@Param("receiverId") Long receiverId,
                   @Param("senderId") Long senderId);

    @Select("SELECT DISTINCT u.*, r.role_name, r.role_code, d.dept_name, p.position_name " +
            "FROM sys_user u " +
            "LEFT JOIN sys_role r ON u.role_id = r.role_id " +
            "LEFT JOIN sys_department d ON u.dept_id = d.dept_id " +
            "LEFT JOIN sys_position p ON u.position_id = p.position_id " +
            "INNER JOIN chat_message m ON (m.sender_id = u.user_id AND m.receiver_id = #{userId}) " +
            "   OR (m.receiver_id = u.user_id AND m.sender_id = #{userId}) " +
            "WHERE u.deleted = 0 AND u.status = 1")
    List<com.oa.attendance.entity.SysUser> selectChatContacts(@Param("userId") Long userId);

    @Select("SELECT m.*, u.real_name AS sender_name, u.avatar AS sender_avatar " +
            "FROM chat_message m " +
            "LEFT JOIN sys_user u ON m.sender_id = u.user_id " +
            "WHERE m.group_id = #{groupId} " +
            "ORDER BY m.create_time ASC")
    List<ChatMessage> selectGroupMessages(@Param("groupId") Long groupId);

    @Select("SELECT m.*, u.real_name AS sender_name, u.avatar AS sender_avatar " +
            "FROM chat_message m " +
            "LEFT JOIN sys_user u ON m.sender_id = u.user_id " +
            "WHERE m.group_id = #{groupId} " +
            "ORDER BY m.create_time DESC LIMIT 1")
    ChatMessage selectLastGroupMessage(@Param("groupId") Long groupId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "INNER JOIN chat_group_member gm ON m.group_id = gm.group_id AND gm.user_id = #{userId} " +
            "WHERE m.group_id = #{groupId} AND m.is_read = 0 AND m.sender_id != #{userId}")
    int countGroupUnread(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Update("UPDATE chat_message SET is_read = 1 WHERE group_id = #{groupId} AND is_read = 0")
    int markGroupRead(@Param("groupId") Long groupId);
}
