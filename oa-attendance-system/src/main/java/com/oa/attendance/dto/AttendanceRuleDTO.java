package com.oa.attendance.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class AttendanceRuleDTO {

    private Long ruleId;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    private Long deptId;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;

    @NotNull(message = "上班时间不能为空")
    private LocalTime workStartTime;

    @NotNull(message = "下班时间不能为空")
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

    @Valid
    private List<RuleWifiDTO> wifiList;

    @Valid
    private List<RuleLocationDTO> locationList;

    @Data
    public static class RuleWifiDTO {
        @NotBlank(message = "WiFi名称不能为空")
        private String wifiSsid;
        private String wifiBssid;
        private Integer enabled;
    }

    @Data
    public static class RuleLocationDTO {
        @NotBlank(message = "地点名称不能为空")
        private String locationName;

        @NotNull(message = "纬度不能为空")
        private java.math.BigDecimal latitude;

        @NotNull(message = "经度不能为空")
        private java.math.BigDecimal longitude;

        private Integer radius;
        private Integer enabled;
    }
}
