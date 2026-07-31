package com.oa.attendance.service;

import com.oa.attendance.dto.ChatSendDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.vo.ChatContactVO;
import com.oa.attendance.vo.ChatMessageVO;

import java.util.List;

public interface IChatService {

    Result<List<ChatContactVO>> getContacts();

    Result<List<ChatMessageVO>> getMessages(Long contactId);

    Result<ChatMessageVO> send(ChatSendDTO dto);
}
