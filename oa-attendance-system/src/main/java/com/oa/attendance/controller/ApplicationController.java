package com.oa.attendance.controller;

import com.oa.attendance.dto.ApplicationCreateDTO;
import com.oa.attendance.dto.ApprovalDecisionDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IApplicationService;
import com.oa.attendance.vo.ApplicationTypeVO;
import com.oa.attendance.vo.ApplicationVO;
import com.oa.attendance.vo.MakeupRecordOptionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@Validated
@CrossOrigin
public class ApplicationController {

    @Autowired
    private IApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasAuthority('approval:submit')")
    public Result<ApplicationVO> submit(@Valid @RequestBody ApplicationCreateDTO dto) {
        return applicationService.submit(dto);
    }

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('approval:self')")
    public Result<List<ApplicationTypeVO>> listApplicationTypes() {
        return applicationService.listApplicationTypes();
    }

    @GetMapping("/makeup-options")
    @PreAuthorize("hasAuthority('approval:self')")
    public Result<List<MakeupRecordOptionVO>> listMakeupRecordOptions() {
        return applicationService.listMakeupRecordOptions();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('approval:self')")
    public Result<List<ApplicationVO>> listMine() {
        return applicationService.listMine();
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyAuthority('approval:self', 'approval:handle', 'approval:all')")
    public Result<ApplicationVO> getDetail(@PathVariable Long applicationId) {
        return applicationService.getDetail(applicationId);
    }

    @GetMapping("/tasks/pending")
    @PreAuthorize("hasAuthority('approval:handle')")
    public Result<List<ApplicationVO>> listPendingTasks() {
        return applicationService.listPendingTasks();
    }

    @GetMapping("/tasks/handled")
    @PreAuthorize("hasAuthority('approval:handle')")
    public Result<List<ApplicationVO>> listHandledTasks() {
        return applicationService.listHandledTasks();
    }

    @PostMapping("/tasks/{taskId}/approve")
    @PreAuthorize("hasAuthority('approval:handle')")
    public Result<ApplicationVO> approve(@PathVariable String taskId,
                                         @Valid @RequestBody(required = false) ApprovalDecisionDTO dto) {
        return applicationService.approve(taskId, dto == null ? new ApprovalDecisionDTO() : dto);
    }

    @PostMapping("/tasks/{taskId}/reject")
    @PreAuthorize("hasAuthority('approval:handle')")
    public Result<ApplicationVO> reject(@PathVariable String taskId,
                                        @Valid @RequestBody ApprovalDecisionDTO dto) {
        return applicationService.reject(taskId, dto);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('approval:all')")
    public Result<List<ApplicationVO>> listAll() {
        return applicationService.listAll();
    }

    @PostMapping("/{applicationId}/cancel")
    @PreAuthorize("hasAuthority('approval:self')")
    public Result<ApplicationVO> cancel(@PathVariable Long applicationId) {
        return applicationService.cancel(applicationId);
    }
}
