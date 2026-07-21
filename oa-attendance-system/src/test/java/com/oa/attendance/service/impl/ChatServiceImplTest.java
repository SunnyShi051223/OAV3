package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.dto.ChatSendDTO;
import com.oa.attendance.entity.ChatMessage;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.ChatMessageMapper;
import com.oa.attendance.mapper.SysRoleMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.vo.ChatContactVO;
import com.oa.attendance.vo.ChatMessageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private DataScopeService dataScopeService;

    @InjectMocks
    private ChatServiceImpl service;

    private SysUser employee;
    private SysUser sameDeptColleague;
    private SysUser otherDeptEmployee;

    @BeforeEach
    void setUp() {
        employee = buildUser(1L, "员工A", 10L, 1L, "EMPLOYEE");
        sameDeptColleague = buildUser(2L, "同事B", 10L, 1L, "EMPLOYEE");
        otherDeptEmployee = buildUser(3L, "其他部门C", 20L, 2L, "EMPLOYEE");
    }

    @Test
    void shouldReturnErrorWhenNotLoggedInForContacts() {
        when(dataScopeService.getCurrentUser()).thenReturn(null);

        Result<List<ChatContactVO>> result = service.getContacts();

        assertNotNull(result);
        assertNotEquals(200, result.getCode());
    }

    @Test
    void employeeShouldOnlyChatWithSameDept() {
        when(dataScopeService.getCurrentUser()).thenReturn(employee);
        when(dataScopeService.hasFullDataAccess()).thenReturn(false);
        when(dataScopeService.hasDepartmentDataAccess()).thenReturn(false);
        when(sysUserMapper.selectAllActiveWithDetails()).thenReturn(
                Arrays.asList(sameDeptColleague, otherDeptEmployee));
        when(chatMessageMapper.selectCount(any())).thenReturn(0L);

        Result<List<ChatContactVO>> result = service.getContacts();

        assertEquals(200, result.getCode());
        // 员工只能看到同部门同事B
        assertEquals(1, result.getData().size());
        assertEquals("同事B", result.getData().get(0).getRealName());
    }

    @Test
    void shouldNotSendToSelf() {
        when(dataScopeService.getCurrentUser()).thenReturn(employee);

        ChatSendDTO dto = new ChatSendDTO();
        dto.setReceiverId(employee.getUserId());
        dto.setContent("你好");

        Result<ChatMessageVO> result = service.send(dto);

        assertNotEquals(200, result.getCode());
    }

    @Test
    void shouldNotSendEmptyOrTooLong() {
        when(dataScopeService.getCurrentUser()).thenReturn(employee);

        ChatSendDTO dto = new ChatSendDTO();
        dto.setReceiverId(sameDeptColleague.getUserId());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2001; i++) {
            sb.append("a");
        }
        dto.setContent(sb.toString());

        Result<ChatMessageVO> result = service.send(dto);

        assertNotEquals(200, result.getCode());
    }

    @Test
    void shouldNotSendToInvisibleUser() {
        when(dataScopeService.getCurrentUser()).thenReturn(employee);
        when(dataScopeService.hasFullDataAccess()).thenReturn(false);
        when(dataScopeService.hasDepartmentDataAccess()).thenReturn(false);
        when(sysUserMapper.selectById(otherDeptEmployee.getUserId())).thenReturn(otherDeptEmployee);

        ChatSendDTO dto = new ChatSendDTO();
        dto.setReceiverId(otherDeptEmployee.getUserId());
        dto.setContent("你好");

        Result<ChatMessageVO> result = service.send(dto);

        assertNotEquals(200, result.getCode());
    }

    @Test
    void shouldSendToVisibleUser() {
        when(dataScopeService.getCurrentUser()).thenReturn(employee);
        when(dataScopeService.hasFullDataAccess()).thenReturn(false);
        when(dataScopeService.hasDepartmentDataAccess()).thenReturn(false);
        when(sysUserMapper.selectById(sameDeptColleague.getUserId())).thenReturn(sameDeptColleague);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        ChatSendDTO dto = new ChatSendDTO();
        dto.setReceiverId(sameDeptColleague.getUserId());
        dto.setContent("你好");

        Result<ChatMessageVO> result = service.send(dto);

        assertEquals(200, result.getCode());
        assertEquals("你好", result.getData().getContent());
    }

    @Test
    void shouldMarkAsReadWhenGettingMessages() {
        when(dataScopeService.getCurrentUser()).thenReturn(employee);
        when(chatMessageMapper.markAsRead(employee.getUserId(), sameDeptColleague.getUserId())).thenReturn(1);
        when(chatMessageMapper.selectConversation(employee.getUserId(), sameDeptColleague.getUserId()))
                .thenReturn(Collections.emptyList());

        Result<List<ChatMessageVO>> result = service.getMessages(sameDeptColleague.getUserId());

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void shouldNotSendToDisabledUser() {
        when(dataScopeService.getCurrentUser()).thenReturn(employee);

        SysUser disabled = buildUser(99L, "禁用", 10L, 1L, "EMPLOYEE");
        disabled.setStatus(0);
        when(sysUserMapper.selectById(99L)).thenReturn(disabled);

        ChatSendDTO dto = new ChatSendDTO();
        dto.setReceiverId(99L);
        dto.setContent("你好");

        Result<ChatMessageVO> result = service.send(dto);

        assertNotEquals(200, result.getCode());
    }

    private SysUser buildUser(Long id, String name, Long deptId, Long roleId, String roleCode) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setRealName(name);
        user.setDeptId(deptId);
        user.setRoleId(roleId);
        user.setRoleCode(roleCode);
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }
}
