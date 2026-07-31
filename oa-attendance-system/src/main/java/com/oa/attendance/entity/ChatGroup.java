package com.oa.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("chat_group")
public class ChatGroup {

    @TableId(value = "group_id", type = IdType.AUTO)
    private Long groupId;

    @TableField("group_name")
    private String groupName;

    @TableField("avatar")
    private String avatar;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField(exist = false)
    private List<ChatGroupMember> members;

    @TableField(exist = false)
    private String lastMessage;

    @TableField(exist = false)
    private String lastMessageTime;

    @TableField(exist = false)
    private Integer unreadCount;
}