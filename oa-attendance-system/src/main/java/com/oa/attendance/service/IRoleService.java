package com.oa.attendance.service;

import com.oa.attendance.dto.RoleCreateDTO;
import com.oa.attendance.dto.RoleMenuAssignDTO;
import com.oa.attendance.dto.RolePermissionAssignDTO;
import com.oa.attendance.dto.RoleUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.vo.RoleListVO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface IRoleService {

    Result<?> create(RoleCreateDTO dto);

    Result<?> update(RoleUpdateDTO dto);

    Result<?> delete(Long roleId);

    Result<RoleListVO> getById(Long roleId);

    Result<List<RoleListVO>> listAll();

    Result<?> assignMenus(RoleMenuAssignDTO dto);

    Result<?> assignPermissions(RolePermissionAssignDTO dto);
}
