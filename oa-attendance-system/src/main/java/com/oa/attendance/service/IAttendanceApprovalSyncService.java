package com.oa.attendance.service;

import com.oa.attendance.entity.AppApplication;
import com.oa.attendance.entity.AttRecord;
import com.oa.attendance.entity.AttRule;

public interface IAttendanceApprovalSyncService {

    void syncApprovedApplication(AppApplication application);

    void refreshRecordAndSummary(AttRecord record, AttRule rule);
}
