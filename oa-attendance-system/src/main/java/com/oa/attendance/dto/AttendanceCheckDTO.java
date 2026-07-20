package com.oa.attendance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AttendanceCheckDTO {

    private Long ruleId;
    private String wifiSsid;
    private String wifiBssid;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationAddress;
    private String clientInfo;
}
