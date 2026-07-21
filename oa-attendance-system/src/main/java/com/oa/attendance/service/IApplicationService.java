package com.oa.attendance.service;

import com.oa.attendance.dto.ApplicationCreateDTO;
import com.oa.attendance.dto.ApprovalDecisionDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.vo.ApplicationTypeVO;
import com.oa.attendance.vo.ApplicationVO;
import com.oa.attendance.vo.MakeupRecordOptionVO;

import java.util.List;

public interface IApplicationService {
    Result<ApplicationVO> submit(ApplicationCreateDTO dto);

    Result<List<ApplicationVO>> listMine();

    Result<ApplicationVO> getDetail(Long applicationId);

    Result<List<ApplicationTypeVO>> listApplicationTypes();

    Result<List<MakeupRecordOptionVO>> listMakeupRecordOptions();

    Result<List<ApplicationVO>> listPendingTasks();

    Result<List<ApplicationVO>> listHandledTasks();

    Result<ApplicationVO> approve(String taskId, ApprovalDecisionDTO dto);

    Result<ApplicationVO> reject(String taskId, ApprovalDecisionDTO dto);

    Result<ApplicationVO> cancel(Long applicationId);

    Result<List<ApplicationVO>> listAll();
}
