package com.oa.attendance.service.impl;

import com.oa.attendance.entity.AppApplication;
import com.oa.attendance.entity.AttDailySummary;
import com.oa.attendance.entity.AttRecord;
import com.oa.attendance.entity.AttRule;
import com.oa.attendance.mapper.AppApplicationMapper;
import com.oa.attendance.mapper.AttDailySummaryMapper;
import com.oa.attendance.mapper.AttRecordMapper;
import com.oa.attendance.mapper.AttRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceApprovalSyncServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 7, 20);

    @Mock
    private AppApplicationMapper applicationMapper;

    @Mock
    private AttRuleMapper ruleMapper;

    @Mock
    private AttRecordMapper recordMapper;

    @Mock
    private AttDailySummaryMapper summaryMapper;

    @InjectMocks
    private AttendanceApprovalSyncServiceImpl service;

    private AttRule rule;

    @BeforeEach
    void setUp() {
        rule = new AttRule();
        rule.setRuleId(1L);
        rule.setRuleName("标准考勤");
        rule.setWorkStartTime(LocalTime.of(9, 0));
        rule.setWorkEndTime(LocalTime.of(18, 0));
        rule.setWorkDays("[1,2,3,4,5]");
        when(summaryMapper.selectOne(any())).thenReturn(null);
    }

    @Test
    void approvedLeaveUpdatesStatusAndMergesOverlappingMinutes() {
        AttRecord record = absentRecord();
        AppApplication fullDay = application("LEAVE", DATE.atTime(8, 0), DATE.atTime(18, 0));
        AppApplication overlapping = application("LEAVE", DATE.atTime(13, 0), DATE.atTime(17, 0));
        when(applicationMapper.selectList(any())).thenReturn(Arrays.asList(fullDay, overlapping));

        service.refreshRecordAndSummary(record, rule);

        AttDailySummary summary = insertedSummary();
        assertEquals("LEAVE", record.getAttendanceStatus());
        assertEquals("LEAVE", summary.getAttendanceStatus());
        assertEquals(540, summary.getLeaveMinutes());
        assertEquals(1, summary.getIsLeave());
        assertEquals(0, summary.getIsAbsent());
    }

    @Test
    void approvedOvertimePreservesPunchesAndSetsApprovedMinutes() {
        AttRecord record = normalRecord();
        AppApplication overtime = application("OVERTIME", DATE.atTime(18, 0), DATE.atTime(20, 0));
        when(applicationMapper.selectList(any())).thenReturn(Collections.singletonList(overtime));

        service.refreshRecordAndSummary(record, rule);

        AttDailySummary summary = insertedSummary();
        assertEquals(DATE.atTime(9, 0), record.getCheckInTime());
        assertEquals(DATE.atTime(18, 0), record.getCheckOutTime());
        assertEquals("OVERTIME", record.getAttendanceStatus());
        assertEquals(120, record.getOvertimeMinutes());
        assertEquals(120, summary.getOvertimeMinutes());
        assertEquals(1, summary.getIsOvertime());
    }

    @Test
    void approvedMakeupFillsMissingPunchesAndClearsAbsence() {
        AttRecord record = absentRecord();
        AppApplication makeup = application("MAKEUP", DATE.atTime(9, 0), DATE.atTime(18, 0));
        makeup.setAttendanceRecordId(record.getRecordId());
        when(applicationMapper.selectList(any())).thenReturn(Collections.singletonList(makeup));

        service.refreshRecordAndSummary(record, rule);

        AttDailySummary summary = insertedSummary();
        assertEquals(DATE.atTime(9, 0), record.getCheckInTime());
        assertEquals(DATE.atTime(18, 0), record.getCheckOutTime());
        assertEquals("NORMAL", record.getCheckInStatus());
        assertEquals("NORMAL", record.getCheckOutStatus());
        assertEquals("NORMAL", record.getAttendanceStatus());
        assertEquals(540, record.getWorkMinutes());
        assertEquals(0, summary.getIsAbsent());
    }

    @Test
    void approvedMakeupIgnoresApplicationForAnotherRuleRecord() {
        AttRecord record = absentRecord();
        AppApplication makeup = application("MAKEUP", DATE.atTime(9, 0), DATE.atTime(18, 0));
        makeup.setAttendanceRecordId(999L);
        when(applicationMapper.selectList(any())).thenReturn(Collections.singletonList(makeup));

        service.refreshRecordAndSummary(record, rule);

        AttDailySummary summary = insertedSummary();
        assertEquals("ABSENT", record.getAttendanceStatus());
        assertEquals(1, summary.getIsAbsent());
    }

    private AttRecord absentRecord() {
        AttRecord record = new AttRecord();
        record.setRecordId(10L);
        record.setUserId(3L);
        record.setDeptId(2L);
        record.setRuleId(1L);
        record.setRuleName("标准考勤");
        record.setAttendanceDate(DATE);
        record.setAttendanceStatus("ABSENT");
        record.setWorkMinutes(0);
        record.setOvertimeMinutes(0);
        return record;
    }

    private AttRecord normalRecord() {
        AttRecord record = absentRecord();
        record.setCheckInTime(DATE.atTime(9, 0));
        record.setCheckOutTime(DATE.atTime(18, 0));
        record.setCheckInStatus("NORMAL");
        record.setCheckOutStatus("NORMAL");
        record.setAttendanceStatus("NORMAL");
        record.setWorkMinutes(540);
        return record;
    }

    private AppApplication application(String type, LocalDateTime start, LocalDateTime end) {
        AppApplication application = new AppApplication();
        application.setApplicantId(3L);
        application.setApplicationType(type);
        application.setStatus("APPROVED");
        application.setStartTime(start);
        application.setEndTime(end);
        return application;
    }

    private AttDailySummary insertedSummary() {
        ArgumentCaptor<AttDailySummary> captor = ArgumentCaptor.forClass(AttDailySummary.class);
        org.mockito.Mockito.verify(summaryMapper).insert(captor.capture());
        return captor.getValue();
    }
}
