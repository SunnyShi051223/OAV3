package com.oa.attendance.vo;

import lombok.Data;

@Data
public class ChatContactVO {

    private Long userId;
    private String realName;
    private String avatar;
    private String deptName;
    private String positionName;
    private String roleName;
    private Integer unreadCount;
    private String lastMessage;
    private String lastMessageTime;
}
