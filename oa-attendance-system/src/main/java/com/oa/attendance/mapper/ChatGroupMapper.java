package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.ChatGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatGroupMapper extends BaseMapper<ChatGroup> {

    @Select("SELECT g.* FROM chat_group g " +
            "INNER JOIN chat_group_member m ON g.group_id = m.group_id " +
            "WHERE m.user_id = #{userId} ORDER BY g.create_time DESC")
    List<ChatGroup> selectByUserId(@Param("userId") Long userId);
}