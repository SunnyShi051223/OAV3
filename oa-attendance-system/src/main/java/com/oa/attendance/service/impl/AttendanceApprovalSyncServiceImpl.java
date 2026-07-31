package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.entity.AppApplication;
import com.oa.attendance.entity.AttDailySummary;
import com.oa.attendance.entity.AttRecord;
import com.oa.attendance.entity.AttRule;
import com.oa.attendance.entity.SysDepartment;
import com.oa.attendance.mapper.AppApplicationMapper;
import com.oa.attendance.mapper.AttDailySummaryMapper;
import com.oa.attendance.mapper.AttRecordMapper;
import com.oa.attendance.mapper.AttRuleMapper;
import com.oa.attendance.mapper.SysDepartmentMapper;
import com.oa.attendance.service.IAttendanceApprovalSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AttendanceApprovalSyncServiceImpl implements IAttendanceApprovalSyncService {

    private static final String APPROVED = "APPROVED";
    private static final String LEAVE = "LEAVE";
    private static final String OVERTIME = "OVERTIME";
    private static final String MAKEUP = "MAKEUP";
    private static final String NORMAL = "NORMAL";
    private static final String LATE = "LATE";
    private static final String EARLY = "EARLY";
    private static final String ABSENT = "ABSENT";

    @Autowired
    private AppApplicationMapper applicationMapper;

    @Autowired
    private AttRuleMapper ruleMapper;

    @Autowired
    private SysDepartmentMapper departmentMapper;

    @Autowired
    private AttRecordMapper recordMapper;

    @Autowired
    private AttDailySummaryMapper summaryMapper;

    @Override
    public void syncApprovedApplication(AppApplication application) {
        if (application == null || !APPROVED.equals(application.getStatus())
                || !isAttendanceApplication(application.getApplicationType())) {
            return;
        }

        if (MAKEUP.equals(application.getApplicationType()) && application.getAttendanceRecordId() != null) {
            AttRecord record = recordMapper.selectById(application.getAttendanceRecordId());
            if (record == null || !application.getApplicantId().equals(record.getUserId())) {
                return;
            }
            AttRule rule = ruleMapper.selectById(record.getRuleId());
            if (rule != null) {
                refreshRecordAndSummary(record, rule);
            }
            return;
        }

        LocalDate cursor = application.getStartTime().toLocalDate();
        LocalDate endDate = application.getEndTime().toLocalDate();
        while (!cursor.isAfter(endDate)) {
            List<AttRule> rules = new ArrayList<>();
            List<Long> deptIds = collectAncestorDeptIds(application.getApplicantDeptId());
            for (Long deptId : deptIds) {
                rules.addAll(ruleMapper.selectAvailableRules(deptId, cursor));
            }
            for (AttRule rule : rules) {
                if (!OVERTIME.equals(application.getApplicationType()) && !isWorkDay(rule, cursor)) {
                    continue;
                }
                AttRecord record = selectRecord(application.getApplicantId(), cursor, rule.getRuleId());
                if (record == null) {
                    record = createRecord(application, rule, cursor);
                    recordMapper.insert(record);
                }
                refreshRecordAndSummary(record, rule);
            }
            cursor = cursor.plusDays(1);
        }
    }

    @Override
    public void refreshRecordAndSummary(AttRecord record, AttRule rule) {
        List<AppApplication> applications = selectApprovedApplications(
                record.getUserId(), record.getAttendanceDate(), record.getRecordId());

        applyMakeupApplications(record, rule, applications);

        AttDailySummary summary = selectSummary(record);
        fillBaseSummary(summary, record, rule);
        applyOvertimeApplications(record, summary, applications);
        applyLeaveApplications(record, rule, summary, applications);

        LocalDateTime now = LocalDateTime.now();
        record.setUpdateTime(now);
        summary.setRecordId(record.getRecordId());
        summary.setUpdateTime(now);
        recordMapper.updateById(record);
        if (summary.getSummaryId() == null) {
            summaryMapper.insert(summary);
        } else {
            summaryMapper.updateById(summary);
        }
    }

    private AttRecord createRecord(AppApplication application, AttRule rule, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        AttRecord record = new AttRecord();
        record.setUserId(application.getApplicantId());
        record.setDeptId(application.getApplicantDeptId());
        record.setRuleId(rule.getRuleId());
        record.setRuleName(rule.getRuleName());
        record.setAttendanceDate(date);
        record.setAttendanceStatus(ABSENT);
        record.setWorkMinutes(0);
        record.setOvertimeMinutes(0);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        return record;
    }

    private AttRecord selectRecord(Long userId, LocalDate date, Long ruleId) {
        QueryWrapper<AttRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("attendance_date", date)
                .eq("rule_id", ruleId);
        return recordMapper.selectOne(wrapper);
    }

    private AttDailySummary selectSummary(AttRecord record) {
        QueryWrapper<AttDailySummary> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", record.getUserId())
                .eq("attendance_date", record.getAttendanceDate())
                .eq("rule_id", record.getRuleId());
        AttDailySummary summary = summaryMapper.selectOne(wrapper);
        if (summary == null) {
            summary = new AttDailySummary();
            summary.setCreateTime(LocalDateTime.now());
        }
        return summary;
    }

    private List<AppApplication> selectApprovedApplications(Long userId, LocalDate date, Long selectingRecordId) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        QueryWrapper<AppApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("applicant_id", userId)
                .eq("status", APPROVED)
                .in("application_type", LEAVE, OVERTIME, MAKEUP)
                .lt("start_time", dayEnd)
                .gt("end_time", dayStart)
                .orderByAsc("create_time");
        List<AppApplication> applications = new ArrayList<>(applicationMapper.selectList(wrapper));
        applications.removeIf(application -> MAKEUP.equals(application.getApplicationType())
                && application.getAttendanceRecordId() != null
                && !application.getAttendanceRecordId().equals(selectingRecordId));
        return applications;
    }

    private void applyMakeupApplications(AttRecord record, AttRule rule,
                                         List<AppApplication> applications) {
        for (AppApplication application : applications) {
            if (!MAKEUP.equals(application.getApplicationType())) {
                continue;
            }
            if (application.getAttendanceRecordId() != null
                    && !application.getAttendanceRecordId().equals(record.getRecordId())) {
                continue;
            }
            LocalDate date = record.getAttendanceDate();
            LocalDateTime checkIn = application.getStartTime().toLocalDate().equals(date)
                    ? application.getStartTime()
                    : LocalDateTime.of(date, rule.getWorkStartTime());
            LocalDateTime checkOut = application.getEndTime().toLocalDate().equals(date)
                    ? application.getEndTime()
                    : LocalDateTime.of(date, rule.getWorkEndTime());

            // 补卡申请直接覆盖签到签退时间
            record.setCheckInTime(checkIn);
            record.setCheckInStatus(NORMAL);
            record.setCheckOutTime(checkOut);
            record.setCheckOutStatus(NORMAL);
            record.setCheckInValid(1);
            record.setCheckOutValid(1);
            record.setCheckInFailReason(null);
            record.setCheckOutFailReason(null);

            // 如果签到时间晚于迟到线，仍标记为迟到
            LocalTime lateLine = rule.getCheckInEndTime() != null
                    ? rule.getCheckInEndTime()
                    : rule.getWorkStartTime().plusMinutes(rule.getLateThreshold() != null ? rule.getLateThreshold() : 10);
            if (checkIn.toLocalTime().isAfter(lateLine)) {
                record.setCheckInStatus(LATE);
            }
        }

        if (record.getCheckInTime() != null && record.getCheckOutTime() != null) {
            int workMinutes = (int) Math.max(0,
                    Duration.between(record.getCheckInTime(), record.getCheckOutTime()).toMinutes());
            record.setWorkMinutes(workMinutes);

            // 计算加班：签退时间晚于下班时间
            int overtimeMinutes = 0;
            if (rule.getWorkEndTime() != null) {
                LocalDateTime workEnd = LocalDateTime.of(record.getAttendanceDate(), rule.getWorkEndTime());
                if (record.getCheckOutTime().isAfter(workEnd)) {
                    overtimeMinutes = (int) Duration.between(workEnd, record.getCheckOutTime()).toMinutes();
                }
            }
            record.setOvertimeMinutes(overtimeMinutes);

            record.setAttendanceStatus(resolveBaseStatus(record));
        }
    }

    private void fillBaseSummary(AttDailySummary summary, AttRecord record, AttRule rule) {
        summary.setUserId(record.getUserId());
        summary.setDeptId(record.getDeptId());
        summary.setAttendanceDate(record.getAttendanceDate());
        summary.setRuleId(record.getRuleId());
        summary.setRecordId(record.getRecordId());
        summary.setAttendanceStatus(record.getAttendanceStatus());
        summary.setWorkMinutes(defaultInt(record.getWorkMinutes()));
        summary.setLateMinutes(LATE.equals(record.getCheckInStatus()) ? calcLateMinutes(record, rule) : 0);
        summary.setEarlyMinutes(EARLY.equals(record.getCheckOutStatus()) ? calcEarlyMinutes(record, rule) : 0);
        summary.setOvertimeMinutes(defaultInt(record.getOvertimeMinutes()));
        summary.setLeaveMinutes(0);
        summary.setIsLate(LATE.equals(record.getCheckInStatus()) ? 1 : 0);
        summary.setIsAbsent(ABSENT.equals(record.getAttendanceStatus()) ? 1 : 0);
        summary.setIsOvertime(OVERTIME.equals(record.getCheckOutStatus()) ? 1 : 0);
        summary.setIsLeave(0);
    }

    private void applyOvertimeApplications(AttRecord record, AttDailySummary summary,
                                           List<AppApplication> applications) {
        List<TimeRange> ranges = collectRanges(applications, OVERTIME,
                record.getAttendanceDate().atStartOfDay(),
                record.getAttendanceDate().plusDays(1).atStartOfDay());
        int approvedMinutes = mergedMinutes(ranges);
        if (approvedMinutes <= 0) {
            return;
        }
        int overtimeMinutes = Math.max(defaultInt(record.getOvertimeMinutes()), approvedMinutes);
        record.setOvertimeMinutes(overtimeMinutes);
        summary.setOvertimeMinutes(overtimeMinutes);
        summary.setIsOvertime(1);
        if (!LEAVE.equals(record.getAttendanceStatus())) {
            record.setAttendanceStatus(OVERTIME);
            summary.setAttendanceStatus(OVERTIME);
        }
    }

    private void applyLeaveApplications(AttRecord record, AttRule rule, AttDailySummary summary,
                                        List<AppApplication> applications) {
        LocalDateTime workStart = LocalDateTime.of(record.getAttendanceDate(), rule.getWorkStartTime());
        LocalDateTime workEnd = LocalDateTime.of(record.getAttendanceDate(), rule.getWorkEndTime());
        if (!workEnd.isAfter(workStart)) {
            workEnd = workEnd.plusDays(1);
        }
        List<TimeRange> ranges = collectRanges(applications, LEAVE, workStart, workEnd);
        int leaveMinutes = mergedMinutes(ranges);
        if (leaveMinutes <= 0) {
            return;
        }

        record.setAttendanceStatus(LEAVE);
        summary.setAttendanceStatus(LEAVE);
        summary.setLeaveMinutes(leaveMinutes);
        summary.setIsLeave(1);
        summary.setIsAbsent(0);

        if (covers(ranges, workStart)) {
            summary.setIsLate(0);
            summary.setLateMinutes(0);
            if (LATE.equals(record.getCheckInStatus())) {
                record.setCheckInStatus(NORMAL);
            }
        }
        if (covers(ranges, workEnd.minusNanos(1))) {
            summary.setEarlyMinutes(0);
            if (EARLY.equals(record.getCheckOutStatus())) {
                record.setCheckOutStatus(NORMAL);
            }
        }
    }

    private List<TimeRange> collectRanges(List<AppApplication> applications, String type,
                                          LocalDateTime boundaryStart, LocalDateTime boundaryEnd) {
        List<TimeRange> ranges = new ArrayList<>();
        for (AppApplication application : applications) {
            if (!type.equals(application.getApplicationType())) {
                continue;
            }
            LocalDateTime start = application.getStartTime().isAfter(boundaryStart)
                    ? application.getStartTime() : boundaryStart;
            LocalDateTime end = application.getEndTime().isBefore(boundaryEnd)
                    ? application.getEndTime() : boundaryEnd;
            if (end.isAfter(start)) {
                ranges.add(new TimeRange(start, end));
            }
        }
        ranges.sort(Comparator.comparing(range -> range.start));
        return ranges;
    }

    private int mergedMinutes(List<TimeRange> ranges) {
        if (ranges.isEmpty()) {
            return 0;
        }
        long minutes = 0;
        LocalDateTime start = ranges.get(0).start;
        LocalDateTime end = ranges.get(0).end;
        for (int i = 1; i < ranges.size(); i++) {
            TimeRange current = ranges.get(i);
            if (!current.start.isAfter(end)) {
                if (current.end.isAfter(end)) {
                    end = current.end;
                }
                continue;
            }
            minutes += Duration.between(start, end).toMinutes();
            start = current.start;
            end = current.end;
        }
        minutes += Duration.between(start, end).toMinutes();
        return (int) Math.min(Integer.MAX_VALUE, minutes);
    }

    private boolean covers(List<TimeRange> ranges, LocalDateTime point) {
        for (TimeRange range : ranges) {
            if (!point.isBefore(range.start) && point.isBefore(range.end)) {
                return true;
            }
        }
        return false;
    }

    private String resolveBaseStatus(AttRecord record) {
        if (LATE.equals(record.getCheckInStatus())) {
            return LATE;
        }
        if (EARLY.equals(record.getCheckOutStatus())) {
            return EARLY;
        }
        if (OVERTIME.equals(record.getCheckOutStatus())) {
            return OVERTIME;
        }
        return NORMAL;
    }

    private int calcLateMinutes(AttRecord record, AttRule rule) {
        if (record.getCheckInTime() == null || rule.getWorkStartTime() == null) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(
                LocalDateTime.of(record.getAttendanceDate(), rule.getWorkStartTime()),
                record.getCheckInTime()).toMinutes());
    }

    private int calcEarlyMinutes(AttRecord record, AttRule rule) {
        if (record.getCheckOutTime() == null || rule.getWorkEndTime() == null) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(
                record.getCheckOutTime(),
                LocalDateTime.of(record.getAttendanceDate(), rule.getWorkEndTime())).toMinutes());
    }

    private boolean isAttendanceApplication(String type) {
        return LEAVE.equals(type) || OVERTIME.equals(type) || MAKEUP.equals(type);
    }

    private boolean isWorkDay(AttRule rule, LocalDate date) {
        String workDays = rule.getWorkDays();
        if (workDays == null || workDays.trim().isEmpty()) {
            return true;
        }
        String day = String.valueOf(date.getDayOfWeek().getValue());
        return workDays.matches(".*(^|[^0-9])" + day + "([^0-9]|$).*");
    }

    private List<Long> collectAncestorDeptIds(Long deptId) {
        List<Long> ids = new ArrayList<>();
        if (deptId == null) return ids;
        ids.add(deptId);
        Long currentId = deptId;
        while (currentId != null && currentId > 0) {
            SysDepartment dept = departmentMapper.selectById(currentId);
            if (dept == null || dept.getParentId() == null || dept.getParentId() == 0) break;
            ids.add(dept.getParentId());
            currentId = dept.getParentId();
        }
        return ids;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static class TimeRange {
        private final LocalDateTime start;
        private final LocalDateTime end;

        private TimeRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }
}
