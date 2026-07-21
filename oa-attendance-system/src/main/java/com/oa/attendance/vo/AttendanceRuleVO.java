package com.oa.attendance.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class AttendanceRuleVO {
    private Long ruleId;
    private String ruleName;
    private Long deptId;
    private String deptName;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private LocalTime checkInStartTime;
    private LocalTime checkInEndTime;
    private LocalTime checkOutStartTime;
    private LocalTime checkOutEndTime;
    private Integer lateThreshold;
    private Integer earlyThreshold;
    private Integer overtimeThreshold;
    private String workDays;
    private Integer requireWifi;
    private Integer requireLocation;
    private Integer enabled;
    private Long createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<RuleWifiVO> wifiList;
    private List<RuleLocationVO> locationList;

    @Data
    public static class RuleWifiVO {
        private Long ruleWifiId;
        private String wifiSsid;
        private String wifiBssid;
        private Integer enabled;
    }

    @Data
    public static class RuleLocationVO {
        private Long ruleLocationId;
        private String locationName;
        private java.math.BigDecimal latitude;
        private java.math.BigDecimal longitude;
        private Integer radius;
        private Integer enabled;
    }
}
