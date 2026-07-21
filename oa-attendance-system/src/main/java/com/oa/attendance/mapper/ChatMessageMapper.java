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
}
