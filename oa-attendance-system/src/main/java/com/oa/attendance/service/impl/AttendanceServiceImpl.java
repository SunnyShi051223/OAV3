package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.dto.AttendanceCheckDTO;
import com.oa.attendance.dto.AttendanceRuleDTO;
import com.oa.attendance.entity.AttCheckLog;
import com.oa.attendance.entity.AttDailySummary;
import com.oa.attendance.entity.AttRecord;
import com.oa.attendance.entity.AttRule;
import com.oa.attendance.entity.AttRuleLocation;
import com.oa.attendance.entity.AttRuleWifi;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysDepartment;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.AttCheckLogMapper;
import com.oa.attendance.mapper.AttDailySummaryMapper;
import com.oa.attendance.mapper.AttRecordMapper;
import com.oa.attendance.mapper.AttRuleLocationMapper;
import com.oa.attendance.mapper.AttRuleMapper;
import com.oa.attendance.mapper.AttRuleWifiMapper;
import com.oa.attendance.mapper.SysDepartmentMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.service.IAttendanceService;
import com.oa.attendance.vo.AttendanceMonthVO;
import com.oa.attendance.vo.AttendanceRecordVO;
import com.oa.attendance.vo.AttendanceRuleVO;
import com.oa.attendance.vo.AttendanceStatsVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
public class AttendanceServiceImpl implements IAttendanceService {

    private static final String CHECK_IN = "CHECK_IN";
    private static final String CHECK_OUT = "CHECK_OUT";
    private static final String NORMAL = "NORMAL";
    private static final String LATE = "LATE";
    private static final String EARLY = "EARLY";
    private static final String ABSENT = "ABSENT";
    private static final String OVERTIME = "OVERTIME";

    @Autowired
    private AttRuleMapper ruleMapper;

    @Autowired
    private AttRuleWifiMapper ruleWifiMapper;

    @Autowired
    private AttRuleLocationMapper ruleLocationMapper;

    @Autowired
    private AttRecordMapper recordMapper;

    @Autowired
    private AttCheckLogMapper checkLogMapper;

    @Autowired
    private AttDailySummaryMapper summaryMapper;

    @Autowired
    private SysDepartmentMapper departmentMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Override
    @Transactional
    public Result<?> createRule(AttendanceRuleDTO dto) {
        SysUser currentUser = requireCurrentUser();
        Long targetDeptId = normalizeRuleDept(dto.getDeptId());
        if (!canMaintainRuleDept(targetDeptId)) {
            return Result.error("只能维护权限范围内的考勤规则");
        }

        AttRule rule = new AttRule();
        BeanUtils.copyProperties(dto, rule);
        rule.setDeptId(targetDeptId);
        fillRuleDefaults(rule);
        rule.setCreatedBy(currentUser.getUserId());
        rule.setDeleted(0);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        ruleMapper.insert(rule);
        saveRuleChildren(rule.getRuleId(), dto);
        return Result.success("创建成功", buildRuleVO(ruleMapper.selectById(rule.getRuleId())));
    }

    @Override
    @Transactional
    public Result<?> updateRule(AttendanceRuleDTO dto) {
        if (dto.getRuleId() == null) {
            return Result.error("规则ID不能为空");
        }
        AttRule existing = ruleMapper.selectById(dto.getRuleId());
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
            return Result.error("考勤规则不存在");
        }
        Long targetDeptId = normalizeRuleDept(dto.getDeptId());
        if (!canMaintainRuleDept(existing.getDeptId()) || !canMaintainRuleDept(targetDeptId)) {
            return Result.error("只能维护权限范围内的考勤规则");
        }

        AttRule rule = new AttRule();
        BeanUtils.copyProperties(dto, rule);
        rule.setDeptId(targetDeptId);
        fillRuleDefaults(rule);
        rule.setDeleted(existing.getDeleted());
        rule.setCreatedBy(existing.getCreatedBy());
        rule.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(rule);

        QueryWrapper<AttRuleWifi> wifiWrapper = new QueryWrapper<>();
        wifiWrapper.eq("rule_id", dto.getRuleId());
        ruleWifiMapper.delete(wifiWrapper);
        QueryWrapper<AttRuleLocation> locationWrapper = new QueryWrapper<>();
        locationWrapper.eq("rule_id", dto.getRuleId());
        ruleLocationMapper.delete(locationWrapper);
        saveRuleChildren(dto.getRuleId(), dto);

        return Result.success("更新成功", buildRuleVO(ruleMapper.selectById(dto.getRuleId())));
    }

    @Override
    @Transactional
    public Result<?> deleteRule(Long ruleId) {
        AttRule rule = ruleMapper.selectById(ruleId);
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted())) {
            return Result.error("考勤规则不存在");
        }
        if (!canMaintainRuleDept(rule.getDeptId())) {
            return Result.error("只能删除权限范围内的考勤规则");
        }
        rule.setDeleted(1);
        rule.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(rule);
        return Result.success("删除成功");
    }

    @Override
    public Result<AttendanceRuleVO> getRule(Long ruleId) {
        AttRule rule = ruleMapper.selectById(ruleId);
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted())) {
            return Result.error("考勤规则不存在");
        }
        if (!canReadRuleDept(rule.getDeptId())) {
            return Result.error("只能查看权限范围内的考勤规则");
        }
        return Result.success("查询成功", buildRuleVO(rule));
    }

    @Override
    public Result<List<AttendanceRuleVO>> listRules(Long deptId) {
        Long scopedDeptId = deptId;
        if (dataScopeService.hasDepartmentDataAccess()) {
            scopedDeptId = dataScopeService.getCurrentDeptId();
        }
        List<AttRule> rules = ruleMapper.selectRuleList(scopedDeptId);
        List<AttendanceRuleVO> list = rules.stream()
                .filter(rule -> canReadRuleDept(rule.getDeptId()))
                .map(this::buildRuleVO)
                .collect(Collectors.toList());
        return Result.success("查询成功", list);
    }

    @Override
    public Result<AttendanceRuleVO> getCurrentRule() {
        SysUser user = requireCurrentUser();
        AttRule rule = ruleMapper.selectActiveRule(user.getDeptId(), LocalDate.now());
        return Result.success("查询成功", rule == null ? null : buildRuleVO(rule));
    }

    @Override
    public Result<List<AttendanceRuleVO>> getAvailableRules() {
        SysUser user = requireCurrentUser();
        LocalDate today = LocalDate.now();
        List<AttendanceRuleVO> rules = ruleMapper.selectAvailableRules(user.getDeptId(), today).stream()
                .filter(rule -> isWorkDay(rule, today))
                .map(this::buildRuleVO)
                .collect(Collectors.toList());
        return Result.success("查询成功", rules);
    }

    @Override
    @Transactional
    public Result<AttendanceRecordVO> checkIn(AttendanceCheckDTO dto) {
        return doCheck(dto, CHECK_IN);
    }

    @Override
    @Transactional
    public Result<AttendanceRecordVO> checkOut(AttendanceCheckDTO dto) {
        return doCheck(dto, CHECK_OUT);
    }

    @Override
    public Result<AttendanceRecordVO> getToday() {
        SysUser user = requireCurrentUser();
        AttRule rule = ruleMapper.selectActiveRule(user.getDeptId(), LocalDate.now());
        AttRecord record = rule == null ? null : selectRecord(user.getUserId(), LocalDate.now(), rule.getRuleId());
        return Result.success("查询成功", record == null ? null : buildRecordVO(recordMapper.selectDetailById(record.getRecordId())));
    }

    @Override
    public Result<List<AttendanceRecordVO>> getTodayRecords() {
        SysUser user = requireCurrentUser();
        QueryWrapper<AttRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getUserId())
                .eq("attendance_date", LocalDate.now())
                .orderByAsc("check_in_time")
                .orderByAsc("record_id");
        List<AttendanceRecordVO> records = recordMapper.selectList(wrapper).stream()
                .map(record -> buildRecordVO(recordMapper.selectDetailById(record.getRecordId())))
                .collect(Collectors.toList());
        return Result.success("查询成功", records);
    }

    @Override
    public Result<AttendanceMonthVO> getMyMonth(String month) {
        SysUser user = requireCurrentUser();
        YearMonth yearMonth = month == null || month.trim().isEmpty() ? YearMonth.now() : YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<AttRecord> records = recordMapper.selectUserCalendar(user.getUserId(), startDate, endDate);
        List<AttendanceRecordVO> calendar = records.stream()
                .map(this::buildRecordVO)
                .collect(Collectors.toList());

        AttendanceMonthVO vo = new AttendanceMonthVO();
        vo.setMonth(yearMonth.toString());
        vo.setCalendar(calendar);
        vo.setWorkMinutes(sumInteger(records.stream().map(AttRecord::getWorkMinutes).collect(Collectors.toList())));
        vo.setOvertimeMinutes(sumInteger(records.stream().map(AttRecord::getOvertimeMinutes).collect(Collectors.toList())));
        vo.setLateCount(countStatus(records, LATE));
        vo.setAbsentCount(countStatus(records, ABSENT));
        vo.setOvertimeCount(countStatus(records, OVERTIME));
        vo.setLeaveCount(countStatus(records, "LEAVE"));
        vo.setLeaveMinutes(0);
        return Result.success("查询成功", vo);
    }

    @Override
    public Result<AttendanceStatsVO> getStats(LocalDate startDate, LocalDate endDate, Long deptId) {
        LocalDate realStart = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate realEnd = endDate == null ? LocalDate.now() : endDate;
        if (realEnd.isBefore(realStart)) {
            return Result.error("结束日期不能早于开始日期");
        }

        Long scopedDeptId = deptId;
        if (dataScopeService.hasDepartmentDataAccess()) {
            scopedDeptId = dataScopeService.getCurrentDeptId();
        }
        if (scopedDeptId != null && !dataScopeService.canAccessDepartment(scopedDeptId)) {
            return Result.error("只能查看权限范围内的考勤统计");
        }

        QueryWrapper<AttDailySummary> wrapper = new QueryWrapper<>();
        wrapper.between("attendance_date", realStart, realEnd);
        if (scopedDeptId != null) {
            wrapper.eq("dept_id", scopedDeptId);
        }
        List<AttDailySummary> summaries = summaryMapper.selectList(wrapper);

        int expectedCount = countExpectedAttendance(realStart, realEnd, scopedDeptId);
        int checkedCount = summaries.size();
        int lateCount = countSummaryFlag(summaries, "is_late");
        int leaveCount = countSummaryFlag(summaries, "is_leave");
        int overtimeCount = countSummaryFlag(summaries, "is_overtime");
        int absentCount = Math.max(0, expectedCount - checkedCount) + countSummaryFlag(summaries, "is_absent");
        int normalCount = Math.max(0, checkedCount - lateCount - leaveCount - countSummaryFlag(summaries, "is_absent"));

        AttendanceStatsVO vo = new AttendanceStatsVO();
        vo.setStartDate(realStart);
        vo.setEndDate(realEnd);
        vo.setDeptId(scopedDeptId);
        vo.setDeptName(getDeptName(scopedDeptId));
        vo.setExpectedCount(expectedCount);
        vo.setNormalCount(normalCount);
        vo.setLateCount(lateCount);
        vo.setAbsentCount(absentCount);
        vo.setLeaveCount(leaveCount);
        vo.setOvertimeCount(overtimeCount);
        vo.setAttendanceRate(formatRate(checkedCount, expectedCount));
        vo.setLateRate(formatRate(lateCount, expectedCount));
        vo.setAbsentRate(formatRate(absentCount, expectedCount));
        vo.setLeaveRate(formatRate(leaveCount, expectedCount));
        return Result.success("查询成功", vo);
    }

    private Result<AttendanceRecordVO> doCheck(AttendanceCheckDTO dto, String checkType) {
        SysUser user = requireCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        AttRule rule = resolveCheckRule(dto, user, today);
        if (rule == null) {
            writeLog(user, null, null, dto, checkType, now, null, null, 0, "请选择今日可用的考勤规则");
            return Result.error("请选择今日可用的考勤规则");
        }
        if (!isWorkDay(rule, today)) {
            writeLog(user, rule, null, dto, checkType, now, 1, 1, 0, "今天不是该规则的工作日");
            return Result.error("今天不是该规则的工作日");
        }

        WindowResult windowResult = validateWindow(rule, checkType, now.toLocalTime());
        ValidationResult validation = validateCheck(dto, rule);
        AttRecord existing = selectRecord(user.getUserId(), today, rule.getRuleId());
        if (CHECK_IN.equals(checkType) && existing != null && existing.getCheckInTime() != null) {
            writeLog(user, rule, existing, dto, checkType, now, validation.wifiValid, validation.locationValid, 0, "今日已签到");
            return Result.error("今日已签到");
        }
        if (CHECK_OUT.equals(checkType) && (existing == null || existing.getCheckInTime() == null)) {
            writeLog(user, rule, existing, dto, checkType, now, validation.wifiValid, validation.locationValid, 0, "请先签到再签退");
            return Result.error("请先签到再签退");
        }
        if (!windowResult.success || !validation.success) {
            String reason = joinReasons(windowResult.reason, validation.reason);
            writeLog(user, rule, existing, dto, checkType, now, validation.wifiValid, validation.locationValid, 0, reason);
            return Result.error(reason);
        }

        AttRecord record = existing == null ? new AttRecord() : existing;
        record.setUserId(user.getUserId());
        record.setDeptId(user.getDeptId());
        record.setRuleId(rule.getRuleId());
        record.setRuleName(rule.getRuleName());
        record.setAttendanceDate(today);
        record.setUpdateTime(now);
        if (record.getCreateTime() == null) {
            record.setCreateTime(now);
        }

        if (CHECK_IN.equals(checkType)) {
            applyCheckIn(record, rule, dto, now, validation);
        } else {
            applyCheckOut(record, rule, dto, now, validation);
        }

        if (record.getRecordId() == null) {
            recordMapper.insert(record);
        } else {
            recordMapper.updateById(record);
        }
        writeLog(user, rule, record, dto, checkType, now, validation.wifiValid, validation.locationValid, 1, null);
        upsertSummary(record, rule);
        return Result.success("打卡成功", buildRecordVO(recordMapper.selectDetailById(record.getRecordId())));
    }

    private void applyCheckIn(AttRecord record, AttRule rule, AttendanceCheckDTO dto, LocalDateTime now, ValidationResult validation) {
        String status = now.toLocalTime().isAfter(rule.getWorkStartTime().plusMinutes(defaultInt(rule.getLateThreshold(), 10))) ? LATE : NORMAL;
        record.setCheckInTime(now);
        record.setCheckInStatus(status);
        record.setAttendanceStatus(status);
        record.setCheckInLocation(dto == null ? null : dto.getLocationAddress());
        record.setCheckInLatitude(dto == null ? null : dto.getLatitude());
        record.setCheckInLongitude(dto == null ? null : dto.getLongitude());
        record.setCheckInWifiSsid(dto == null ? null : dto.getWifiSsid());
        record.setCheckInWifiBssid(dto == null ? null : dto.getWifiBssid());
        record.setCheckInDistance(validation.distance);
        record.setCheckInValid(1);
        record.setCheckInFailReason(null);
    }

    private void applyCheckOut(AttRecord record, AttRule rule, AttendanceCheckDTO dto, LocalDateTime now, ValidationResult validation) {
        LocalTime checkOutTime = now.toLocalTime();
        LocalTime earlyLine = rule.getWorkEndTime().minusMinutes(defaultInt(rule.getEarlyThreshold(), 10));
        LocalTime overtimeLine = rule.getWorkEndTime().plusMinutes(defaultInt(rule.getOvertimeThreshold(), 30));
        String checkOutStatus = NORMAL;
        if (checkOutTime.isBefore(earlyLine)) {
            checkOutStatus = EARLY;
        } else if (checkOutTime.isAfter(overtimeLine)) {
            checkOutStatus = OVERTIME;
        }

        record.setCheckOutTime(now);
        record.setCheckOutStatus(checkOutStatus);
        record.setCheckOutLocation(dto == null ? null : dto.getLocationAddress());
        record.setCheckOutLatitude(dto == null ? null : dto.getLatitude());
        record.setCheckOutLongitude(dto == null ? null : dto.getLongitude());
        record.setCheckOutWifiSsid(dto == null ? null : dto.getWifiSsid());
        record.setCheckOutWifiBssid(dto == null ? null : dto.getWifiBssid());
        record.setCheckOutDistance(validation.distance);
        record.setCheckOutValid(1);
        record.setCheckOutFailReason(null);
        if (record.getCheckInTime() != null) {
            record.setWorkMinutes((int) Math.max(0, Duration.between(record.getCheckInTime(), now).toMinutes()));
        }
        int overtimeMinutes = (int) Math.max(0, Duration.between(LocalDateTime.of(now.toLocalDate(), rule.getWorkEndTime()), now).toMinutes());
        record.setOvertimeMinutes(OVERTIME.equals(checkOutStatus) ? overtimeMinutes : 0);
        if (LATE.equals(record.getCheckInStatus())) {
            record.setAttendanceStatus(LATE);
        } else {
            record.setAttendanceStatus(checkOutStatus);
        }
    }

    private ValidationResult validateCheck(AttendanceCheckDTO dto, AttRule rule) {
        ValidationResult result = new ValidationResult();
        result.wifiValid = 1;
        result.locationValid = 1;
        result.success = true;

        if (Integer.valueOf(1).equals(rule.getRequireWifi())) {
            List<AttRuleWifi> wifiList = getWifiList(rule.getRuleId());
            result.wifiValid = isWifiValid(dto, wifiList) ? 1 : 0;
            if (result.wifiValid == 0) {
                result.success = false;
                result.reason = joinReasons(result.reason, "WiFi不在允许范围内");
            }
        }

        if (Integer.valueOf(1).equals(rule.getRequireLocation())) {
            List<AttRuleLocation> locationList = getLocationList(rule.getRuleId());
            LocationCheck locationCheck = checkLocation(dto, locationList);
            result.locationValid = locationCheck.valid ? 1 : 0;
            result.distance = locationCheck.distance;
            if (!locationCheck.valid) {
                result.success = false;
                result.reason = joinReasons(result.reason, locationCheck.reason);
            }
        }
        return result;
    }

    private boolean isWifiValid(AttendanceCheckDTO dto, List<AttRuleWifi> wifiList) {
        if (dto == null || isBlank(dto.getWifiSsid()) || wifiList.isEmpty()) {
            return false;
        }
        for (AttRuleWifi wifi : wifiList) {
            boolean ssidMatch = dto.getWifiSsid().equalsIgnoreCase(wifi.getWifiSsid());
            boolean bssidMatch = isBlank(wifi.getWifiBssid()) || (!isBlank(dto.getWifiBssid())
                    && dto.getWifiBssid().equalsIgnoreCase(wifi.getWifiBssid()));
            if (ssidMatch && bssidMatch) {
                return true;
            }
        }
        return false;
    }

    private LocationCheck checkLocation(AttendanceCheckDTO dto, List<AttRuleLocation> locationList) {
        LocationCheck result = new LocationCheck();
        result.valid = false;
        if (dto == null || dto.getLatitude() == null || dto.getLongitude() == null) {
            result.reason = "未获取到定位信息";
            return result;
        }
        if (locationList.isEmpty()) {
            result.reason = "规则未配置有效办公地点";
            return result;
        }
        Integer nearest = null;
        for (AttRuleLocation location : locationList) {
            int distance = distanceMeters(dto.getLatitude(), dto.getLongitude(), location.getLatitude(), location.getLongitude());
            if (nearest == null || distance < nearest) {
                nearest = distance;
            }
            int radius = defaultInt(location.getRadius(), 100);
            if (distance <= radius) {
                result.valid = true;
            }
        }
        result.distance = nearest;
        if (!result.valid) {
            result.reason = "定位不在允许范围内";
        }
        return result;
    }

    private WindowResult validateWindow(AttRule rule, String checkType, LocalTime now) {
        WindowResult result = new WindowResult();
        result.success = true;
        LocalTime start = CHECK_IN.equals(checkType) ? rule.getCheckInStartTime() : rule.getCheckOutStartTime();
        LocalTime end = CHECK_IN.equals(checkType) ? rule.getCheckInEndTime() : rule.getCheckOutEndTime();
        if (start != null && now.isBefore(start)) {
            result.success = false;
            result.reason = "未到允许打卡时间";
        }
        if (end != null && now.isAfter(end)) {
            result.success = false;
            result.reason = "已超过允许打卡时间";
        }
        return result;
    }

    private void upsertSummary(AttRecord record, AttRule rule) {
        QueryWrapper<AttDailySummary> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", record.getUserId())
                .eq("attendance_date", record.getAttendanceDate())
                .eq("rule_id", record.getRuleId());
        AttDailySummary summary = summaryMapper.selectOne(wrapper);
        if (summary == null) {
            summary = new AttDailySummary();
            summary.setCreateTime(LocalDateTime.now());
        }
        summary.setUserId(record.getUserId());
        summary.setDeptId(record.getDeptId());
        summary.setAttendanceDate(record.getAttendanceDate());
        summary.setRuleId(record.getRuleId());
        summary.setRecordId(record.getRecordId());
        summary.setAttendanceStatus(record.getAttendanceStatus());
        summary.setWorkMinutes(defaultInt(record.getWorkMinutes(), 0));
        summary.setLateMinutes(calcLateMinutes(record, rule));
        summary.setEarlyMinutes(calcEarlyMinutes(record, rule));
        summary.setOvertimeMinutes(defaultInt(record.getOvertimeMinutes(), 0));
        summary.setLeaveMinutes(0);
        summary.setIsLate(LATE.equals(record.getCheckInStatus()) ? 1 : 0);
        summary.setIsAbsent(ABSENT.equals(record.getAttendanceStatus()) ? 1 : 0);
        summary.setIsOvertime(OVERTIME.equals(record.getCheckOutStatus()) ? 1 : 0);
        summary.setIsLeave("LEAVE".equals(record.getAttendanceStatus()) ? 1 : 0);
        summary.setUpdateTime(LocalDateTime.now());
        if (summary.getSummaryId() == null) {
            summaryMapper.insert(summary);
        } else {
            summaryMapper.updateById(summary);
        }
    }

    private void writeLog(SysUser user, AttRule rule, AttRecord record, AttendanceCheckDTO dto, String checkType,
                          LocalDateTime now, Integer wifiValid, Integer locationValid, Integer success, String failReason) {
        AttCheckLog log = new AttCheckLog();
        log.setUserId(user == null ? null : user.getUserId());
        log.setRuleId(rule == null ? null : rule.getRuleId());
        log.setRecordId(record == null ? null : record.getRecordId());
        log.setAttendanceDate(now.toLocalDate());
        log.setCheckType(checkType);
        log.setCheckTime(now);
        log.setWifiSsid(dto == null ? null : dto.getWifiSsid());
        log.setWifiBssid(dto == null ? null : dto.getWifiBssid());
        log.setLatitude(dto == null ? null : dto.getLatitude());
        log.setLongitude(dto == null ? null : dto.getLongitude());
        log.setLocationAddress(dto == null ? null : dto.getLocationAddress());
        log.setDistance(recordDistance(record, checkType));
        log.setWifiValid(wifiValid);
        log.setLocationValid(locationValid);
        log.setSuccess(success);
        log.setFailReason(failReason);
        log.setClientInfo(dto == null ? null : dto.getClientInfo());
        log.setCreateTime(now);
        checkLogMapper.insert(log);
    }

    private Integer recordDistance(AttRecord record, String checkType) {
        if (record == null) {
            return null;
        }
        return CHECK_IN.equals(checkType) ? record.getCheckInDistance() : record.getCheckOutDistance();
    }

    private void saveRuleChildren(Long ruleId, AttendanceRuleDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        List<AttendanceRuleDTO.RuleWifiDTO> wifiList = dto.getWifiList() == null ? Collections.emptyList() : dto.getWifiList();
        for (AttendanceRuleDTO.RuleWifiDTO item : wifiList) {
            AttRuleWifi wifi = new AttRuleWifi();
            BeanUtils.copyProperties(item, wifi);
            wifi.setRuleId(ruleId);
            wifi.setEnabled(item.getEnabled() == null ? 1 : item.getEnabled());
            wifi.setCreateTime(now);
            wifi.setUpdateTime(now);
            ruleWifiMapper.insert(wifi);
        }
        List<AttendanceRuleDTO.RuleLocationDTO> locationList = dto.getLocationList() == null ? Collections.emptyList() : dto.getLocationList();
        for (AttendanceRuleDTO.RuleLocationDTO item : locationList) {
            AttRuleLocation location = new AttRuleLocation();
            BeanUtils.copyProperties(item, location);
            location.setRuleId(ruleId);
            location.setRadius(item.getRadius() == null ? 100 : item.getRadius());
            location.setEnabled(item.getEnabled() == null ? 1 : item.getEnabled());
            location.setCreateTime(now);
            location.setUpdateTime(now);
            ruleLocationMapper.insert(location);
        }
    }

    private AttendanceRuleVO buildRuleVO(AttRule rule) {
        AttendanceRuleVO vo = new AttendanceRuleVO();
        BeanUtils.copyProperties(rule, vo);
        vo.setDeptName(rule.getDeptName() == null ? getDeptName(rule.getDeptId()) : rule.getDeptName());
        vo.setWifiList(getWifiList(rule.getRuleId()).stream().map(item -> {
            AttendanceRuleVO.RuleWifiVO child = new AttendanceRuleVO.RuleWifiVO();
            BeanUtils.copyProperties(item, child);
            return child;
        }).collect(Collectors.toList()));
        vo.setLocationList(getLocationList(rule.getRuleId()).stream().map(item -> {
            AttendanceRuleVO.RuleLocationVO child = new AttendanceRuleVO.RuleLocationVO();
            BeanUtils.copyProperties(item, child);
            return child;
        }).collect(Collectors.toList()));
        return vo;
    }

    private AttendanceRecordVO buildRecordVO(AttRecord record) {
        AttendanceRecordVO vo = new AttendanceRecordVO();
        BeanUtils.copyProperties(record, vo);
        vo.setDeptName(record.getDeptName() == null ? getDeptName(record.getDeptId()) : record.getDeptName());
        if (record.getRealName() == null && record.getUserId() != null) {
            SysUser user = userMapper.selectById(record.getUserId());
            vo.setRealName(user == null ? null : user.getRealName());
        }
        return vo;
    }

    private List<AttRuleWifi> getWifiList(Long ruleId) {
        QueryWrapper<AttRuleWifi> wrapper = new QueryWrapper<>();
        wrapper.eq("rule_id", ruleId).eq("enabled", 1);
        return ruleWifiMapper.selectList(wrapper);
    }

    private List<AttRuleLocation> getLocationList(Long ruleId) {
        QueryWrapper<AttRuleLocation> wrapper = new QueryWrapper<>();
        wrapper.eq("rule_id", ruleId).eq("enabled", 1);
        return ruleLocationMapper.selectList(wrapper);
    }

    private AttRecord selectRecord(Long userId, LocalDate date, Long ruleId) {
        QueryWrapper<AttRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("attendance_date", date).eq("rule_id", ruleId);
        return recordMapper.selectOne(wrapper);
    }

    private AttRule resolveCheckRule(AttendanceCheckDTO dto, SysUser user, LocalDate date) {
        Long ruleId = dto == null ? null : dto.getRuleId();
        if (ruleId == null) {
            return ruleMapper.selectActiveRule(user.getDeptId(), date);
        }
        AttRule rule = ruleMapper.selectById(ruleId);
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted()) || !Integer.valueOf(1).equals(rule.getEnabled())) {
            return null;
        }
        if (rule.getDeptId() != null && !rule.getDeptId().equals(user.getDeptId())) {
            return null;
        }
        if (rule.getEffectiveStartDate() != null && rule.getEffectiveStartDate().isAfter(date)) {
            return null;
        }
        if (rule.getEffectiveEndDate() != null && rule.getEffectiveEndDate().isBefore(date)) {
            return null;
        }
        return rule;
    }

    private Long normalizeRuleDept(Long deptId) {
        if (dataScopeService.hasDepartmentDataAccess()) {
            return dataScopeService.getCurrentDeptId();
        }
        return deptId;
    }

    private boolean canMaintainRuleDept(Long deptId) {
        if (dataScopeService.hasFullDataAccess()) {
            return true;
        }
        return deptId != null && dataScopeService.canAccessDepartment(deptId);
    }

    private boolean canReadRuleDept(Long deptId) {
        if (dataScopeService.hasFullDataAccess()) {
            return true;
        }
        if (deptId == null) {
            return false;
        }
        return dataScopeService.canAccessDepartment(deptId);
    }

    private SysUser requireCurrentUser() {
        SysUser user = dataScopeService.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("未获取到当前登录用户");
        }
        return user;
    }

    private void fillRuleDefaults(AttRule rule) {
        if (rule.getLateThreshold() == null) {
            rule.setLateThreshold(10);
        }
        if (rule.getEarlyThreshold() == null) {
            rule.setEarlyThreshold(10);
        }
        if (rule.getOvertimeThreshold() == null) {
            rule.setOvertimeThreshold(30);
        }
        if (rule.getRequireWifi() == null) {
            rule.setRequireWifi(1);
        }
        if (rule.getRequireLocation() == null) {
            rule.setRequireLocation(1);
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        if (isBlank(rule.getWorkDays())) {
            rule.setWorkDays("[1,2,3,4,5]");
        }
    }

    private boolean isWorkDay(AttRule rule, LocalDate date) {
        String workDays = rule.getWorkDays();
        if (isBlank(workDays)) {
            return true;
        }
        int day = date.getDayOfWeek().getValue();
        return workDays.contains(String.valueOf(day));
    }

    private int calcLateMinutes(AttRecord record, AttRule rule) {
        if (record.getCheckInTime() == null || rule.getWorkStartTime() == null) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(LocalDateTime.of(record.getAttendanceDate(), rule.getWorkStartTime()), record.getCheckInTime()).toMinutes());
    }

    private int calcEarlyMinutes(AttRecord record, AttRule rule) {
        if (record.getCheckOutTime() == null || rule.getWorkEndTime() == null) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(record.getCheckOutTime(), LocalDateTime.of(record.getAttendanceDate(), rule.getWorkEndTime())).toMinutes());
    }

    private int countExpectedAttendance(LocalDate startDate, LocalDate endDate, Long deptId) {
        QueryWrapper<SysUser> userWrapper = new QueryWrapper<>();
        userWrapper.eq("deleted", 0).eq("status", 1);
        if (deptId != null) {
            userWrapper.eq("dept_id", deptId);
        }
        Long userCount = userMapper.selectCount(userWrapper);
        return userCount.intValue() * countWeekdays(startDate, endDate);
    }

    private int countWeekdays(LocalDate startDate, LocalDate endDate) {
        int count = 0;
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            DayOfWeek day = cursor.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }

    private int countSummaryFlag(List<AttDailySummary> summaries, String flag) {
        int count = 0;
        for (AttDailySummary summary : summaries) {
            if ("is_late".equals(flag) && Integer.valueOf(1).equals(summary.getIsLate())) {
                count++;
            } else if ("is_absent".equals(flag) && Integer.valueOf(1).equals(summary.getIsAbsent())) {
                count++;
            } else if ("is_leave".equals(flag) && Integer.valueOf(1).equals(summary.getIsLeave())) {
                count++;
            } else if ("is_overtime".equals(flag) && Integer.valueOf(1).equals(summary.getIsOvertime())) {
                count++;
            }
        }
        return count;
    }

    private int countStatus(List<AttRecord> records, String status) {
        int count = 0;
        for (AttRecord record : records) {
            if (status.equals(record.getAttendanceStatus()) || status.equals(record.getCheckInStatus()) || status.equals(record.getCheckOutStatus())) {
                count++;
            }
        }
        return count;
    }

    private int sumInteger(List<Integer> values) {
        int sum = 0;
        for (Integer value : values) {
            sum += defaultInt(value, 0);
        }
        return sum;
    }

    private String getDeptName(Long deptId) {
        if (deptId == null) {
            return "全公司";
        }
        SysDepartment dept = departmentMapper.selectById(deptId);
        return dept == null ? null : dept.getDeptName();
    }

    private String formatRate(int count, int expected) {
        if (expected <= 0) {
            return "0.00%";
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(expected), 2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    private int distanceMeters(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double earthRadius = 6371000D;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) Math.round(earthRadius * c);
    }

    private String joinReasons(String first, String second) {
        StringJoiner joiner = new StringJoiner("；");
        if (!isBlank(first)) {
            joiner.add(first);
        }
        if (!isBlank(second)) {
            joiner.add(second);
        }
        return joiner.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static class ValidationResult {
        private boolean success;
        private Integer wifiValid;
        private Integer locationValid;
        private Integer distance;
        private String reason;
    }

    private static class LocationCheck {
        private boolean valid;
        private Integer distance;
        private String reason;
    }

    private static class WindowResult {
        private boolean success;
        private String reason;
    }
}
