package com.oa.attendance.service.impl;

import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.service.IColleagueService;
import com.oa.attendance.vo.ColleagueVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ColleagueServiceImpl implements IColleagueService {

    private static final String ROLE_MANAGER = "MANAGER";

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Override
    public Result<List<ColleagueVO>> search(String keyword) {
        SysUser currentUser = dataScopeService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success("查询成功", java.util.Collections.emptyList());
        }

        List<SysUser> users = sysUserMapper.searchColleagues(keyword.trim());

        List<ColleagueVO> result = users.stream()
                .filter(user -> !user.getUserId().equals(currentUser.getUserId()))
                .filter(user -> canViewColleague(currentUser, user))
                .map(this::toVO)
                .collect(Collectors.toList());

        return Result.success("查询成功", result);
    }

    private boolean canViewColleague(SysUser currentUser, SysUser target) {
        return true;
    }

    private ColleagueVO toVO(SysUser user) {
        ColleagueVO vo = new ColleagueVO();
        vo.setUserId(user.getUserId());
        vo.setEmployeeNo(user.getEmployeeNo());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setHireDate(user.getHireDate());
        vo.setDeptId(user.getDeptId());
        vo.setDeptName(user.getDeptName());
        vo.setPositionId(user.getPositionId());
        vo.setPositionName(user.getPositionName());
        vo.setRoleName(user.getRoleName());
        return vo;
    }
}
