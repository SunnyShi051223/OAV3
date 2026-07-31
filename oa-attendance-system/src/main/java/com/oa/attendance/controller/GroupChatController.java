package com.oa.attendance.controller;

import com.oa.attendance.entity.ChatGroup;
import com.oa.attendance.entity.ChatGroupMember;
import com.oa.attendance.entity.Result;
import com.oa.attendance.service.impl.GroupChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group-chat")
@CrossOrigin
public class GroupChatController {

    @Autowired
    private GroupChatService groupChatService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('chat:query')")
    public Result<ChatGroup> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("groupName");
        @SuppressWarnings("unchecked")
        List<Object> rawMembers = (List<Object>) body.get("memberIds");
        List<Long> members = new ArrayList<>();
        if (rawMembers != null) {
            for (Object id : rawMembers) {
                members.add(((Number) id).longValue());
            }
        }
        return groupChatService.createGroup(name, members);
    }

    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('chat:query')")
    public Result<List<Map<String, Object>>> listMyGroups() {
        return groupChatService.listMyGroups();
    }

    @GetMapping("/{groupId}/messages")
    @PreAuthorize("hasAuthority('chat:query')")
    public Result<List<Map<String, Object>>> getMessages(@PathVariable Long groupId) {
        return groupChatService.getGroupMessages(groupId);
    }

    @PostMapping("/{groupId}/send")
    @PreAuthorize("hasAuthority('chat:query')")
    public Result<Map<String, Object>> send(@PathVariable Long groupId, @RequestBody Map<String, String> body) {
        return groupChatService.sendGroupMessage(groupId, body.get("content"));
    }

    @GetMapping("/{groupId}/members")
    @PreAuthorize("hasAuthority('chat:query')")
    public Result<List<ChatGroupMember>> getMembers(@PathVariable Long groupId) {
        return groupChatService.getGroupMembers(groupId);
    }

    @PostMapping("/{groupId}/add-members")
    @PreAuthorize("hasAuthority('chat:query')")
    public Result<?> addMembers(@PathVariable Long groupId, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.get("userIds");
        List<Long> userIds = new ArrayList<>();
        if (rawIds != null) {
            for (Object id : rawIds) {
                userIds.add(((Number) id).longValue());
            }
        }
        return groupChatService.addMembers(groupId, userIds);
    }

    @PostMapping("/{groupId}/leave")
    @PreAuthorize("hasAuthority('chat:query')")
    public Result<?> leave(@PathVariable Long groupId) {
        return groupChatService.leaveGroup(groupId);
    }
}