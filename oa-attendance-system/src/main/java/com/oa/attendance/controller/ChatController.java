package com.oa.attendance.controller;

import com.oa.attendance.dto.ChatSendDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IChatService;
import com.oa.attendance.vo.ChatContactVO;
import com.oa.attendance.vo.ChatMessageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Validated
@CrossOrigin
public class ChatController {

    @Autowired
    private IChatService chatService;

    @GetMapping("/contacts")
    @PreAuthorize("hasAuthority('chat:query')")
    public Result<List<ChatContactVO>> getContacts() {
        return chatService.getContacts();
    }

    @GetMapping("/messages/{contactId}")
    @PreAuthorize("hasAuthority('chat:query')")
    public Result<List<ChatMessageVO>> getMessages(@PathVariable Long contactId) {
        return chatService.getMessages(contactId);
    }

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('chat:query')")
    public Result<ChatMessageVO> send(@Valid @RequestBody ChatSendDTO dto) {
        return chatService.send(dto);
    }
}
