package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.ChatGroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatGroupMemberMapper extends BaseMapper<ChatGroupMember> {

    @Select("SELECT m.*, u.real_name, u.avatar FROM chat_group_member m " +
            "LEFT JOIN sys_user u ON m.user_id = u.user_id " +
            "WHERE m.group_id = #{groupId}")
    List<ChatGroupMember> selectByGroupId(@Param("groupId") Long groupId);

    @Select("SELECT user_id FROM chat_group_member WHERE group_id = #{groupId} AND user_id != #{excludeUserId}")
    List<Long> selectMemberIdsExcept(@Param("groupId") Long groupId, @Param("excludeUserId") Long excludeUserId);
}