package com.oa.attendance.service.impl;

import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysRole;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.SysRoleMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.vo.ColleagueVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColleagueServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private DataScopeService dataScopeService;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @InjectMocks
    private ColleagueServiceImpl service;

    private SysUser employee;
    private SysUser sameDeptColleague;
    private SysUser otherDeptEmployee;
    private SysUser otherDeptManager;
    private SysUser admin;

    @BeforeEach
    void setUp() {
        employee = buildUser(1L, "员工A", 10L, 1L, "EMPLOYEE");
        sameDeptColleague = buildUser(2L, "同事B", 10L, 1L, "EMPLOYEE");
        otherDeptEmployee = buildUser(3L, "其他部门员工C", 20L, 2L, "EMPLOYEE");
        otherDeptManager = buildUser(4L, "其他部门主管D", 20L, 3L, "MANAGER");
        admin = buildUser(5L, "管理员E", 30L, 4L, "ADMIN");
    }

    @Test
    void shouldReturnEmptyForBlankKeyword() {
        when(dataScopeService.getCurrentUser()).thenReturn(employee);

        Result<List<ColleagueVO>> result = service.search("  ");

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullKeyword() {
        when(dataScopeService.getCurrentUser()).thenReturn(employee);

        Result<List<ColleagueVO>> result = service.search(null);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void shouldReturnErrorWhenNotLoggedIn() {
        when(dataScopeService.getCurrentUser()).thenReturn(null);

        Result<List<ColleagueVO>> result = service.search("张三");

        assertNotNull(result);
        assertNotEquals(200, result.getCode());
    }

    @Test
    void employeeShouldOnlySeeSameDept() {
        when(dataScopeService.getCurrentUser()).thenReturn(employee);
        when(dataScopeService.hasFullDataAccess()).thenReturn(false);
        when(dataScopeService.hasDepartmentDataAccess()).thenReturn(false);
        when(sysUserMapper.searchColleagues("测试")).thenReturn(
                Arrays.asList(sameDeptColleague, otherDeptEmployee, otherDeptManager, admin));

        Result<List<ColleagueVO>> result = service.search("测试");

        assertEquals(200, result.getCode());
        // 员工只能看到同部门同事(同事B)
        assertEquals(1, result.getData().size());
        assertEquals("同事B", result.getData().get(0).getRealName());
    }

    @Test
    void managerShouldSeeSameDeptAndOtherManagers() {
        SysUser manager = buildUser(6L, "主管", 10L, 3L, "MANAGER");
        when(dataScopeService.getCurrentUser()).thenReturn(manager);
        when(dataScopeService.hasFullDataAccess()).thenReturn(false);
        when(dataScopeService.hasDepartmentDataAccess()).thenReturn(true);
        when(sysUserMapper.searchColleagues("测试")).thenReturn(
                Arrays.asList(sameDeptColleague, otherDeptEmployee, otherDeptManager, admin));

        Result<List<ColleagueVO>> result = service.search("测试");

        assertEquals(200, result.getCode());
        // 主管看到：同部门同事B + 其他部门主管D (看不到其他部门员工C 和 管理员E)
        List<ColleagueVO> data = result.getData();
        assertEquals(2, data.size());
        assertTrue(data.stream().anyMatch(v -> "同事B".equals(v.getRealName())));
        assertTrue(data.stream().anyMatch(v -> "其他部门主管D".equals(v.getRealName())));
    }

    @Test
    void adminShouldSeeEveryone() {
        when(dataScopeService.getCurrentUser()).thenReturn(admin);
        when(dataScopeService.hasFullDataAccess()).thenReturn(true);
        List<SysUser> allUsers = Arrays.asList(
                employee, sameDeptColleague, otherDeptEmployee, otherDeptManager);
        when(sysUserMapper.searchColleagues("测试")).thenReturn(allUsers);

        Result<List<ColleagueVO>> result = service.search("测试");

        assertEquals(200, result.getCode());
        // 管理员能看到所有4人(排除自己)
        assertEquals(4, result.getData().size());
    }

    @Test
    void shouldExcludeCurrentUser() {
        when(dataScopeService.getCurrentUser()).thenReturn(employee);
        when(dataScopeService.hasFullDataAccess()).thenReturn(true);
        when(sysUserMapper.searchColleagues("测试")).thenReturn(
                Arrays.asList(employee, sameDeptColleague));

        Result<List<ColleagueVO>> result = service.search("测试");

        // 不应该包含自己
        assertEquals(1, result.getData().size());
        assertEquals("同事B", result.getData().get(0).getRealName());
    }

    private SysUser buildUser(Long id, String name, Long deptId, Long roleId, String roleCode) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setRealName(name);
        user.setDeptId(deptId);
        user.setRoleId(roleId);
        user.setRoleCode(roleCode);
        user.setEmployeeNo("EMP" + id);
        return user;
    }
}
