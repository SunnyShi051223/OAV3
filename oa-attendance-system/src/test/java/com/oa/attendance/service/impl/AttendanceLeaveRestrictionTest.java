package com.oa.attendance.service.impl;

import com.oa.attendance.dto.AttendanceCheckDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.AppApplicationMapper;
import com.oa.attendance.mapper.AttCheckLogMapper;
import com.oa.attendance.mapper.AttRuleMapper;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.vo.AttendanceRecordVO;
import com.oa.attendance.vo.AttendanceRuleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceLeaveRestrictionTest {

    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private AppApplicationMapper applicationMapper;
    @Mock
    private AttRuleMapper ruleMapper;
    @Mock
    private AttCheckLogMapper checkLogMapper;

    @InjectMocks
    private AttendanceServiceImpl service;

    @BeforeEach
    void setUp() {
        SysUser employee = new SysUser();
        employee.setUserId(3L);
        employee.setDeptId(2L);
        when(dataScopeService.getCurrentUser()).thenReturn(employee);
        when(applicationMapper.countApprovedLeaveAt(eq(3L), any(LocalDateTime.class))).thenReturn(1);
    }

    @Test
    void approvedLeaveHidesAvailableRules() {
        Result<List<AttendanceRuleVO>> result = service.getAvailableRules();

        assertTrue(result.getData().isEmpty());
        assertEquals("当前处于已批准请假时段，无需打卡", result.getMsg());
        verifyNoInteractions(ruleMapper);
    }

    @Test
    void approvedLeaveRejectsDirectCheckRequest() {
        Result<AttendanceRecordVO> result = service.checkIn(new AttendanceCheckDTO());

        assertEquals("当前处于已批准请假时段，无需打卡", result.getMsg());
        verifyNoInteractions(ruleMapper);
    }
}
