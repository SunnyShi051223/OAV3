package com.oa.attendance.service.impl;

import com.oa.attendance.dto.UserCreateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysDepartment;
import com.oa.attendance.entity.SysPosition;
import com.oa.attendance.entity.SysRole;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.mapper.SysDepartmentMapper;
import com.oa.attendance.mapper.SysPositionMapper;
import com.oa.attendance.mapper.SysRoleMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysDepartmentMapper sysDepartmentMapper;
    @Mock
    private SysPositionMapper sysPositionMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private DataScopeService dataScopeService;

    @InjectMocks
    private UserServiceImpl service;

    private UserCreateDTO dto;

    @BeforeEach
    void setUp() {
        dto = new UserCreateDTO();
        dto.setEmployeeNo("E0002");
        dto.setUsername("new.employee");
        dto.setPassword("123456");
        dto.setRealName("新员工");
        dto.setDeptId(2L);
        dto.setPositionId(1L);
        dto.setRoleId(1L);
        dto.setStatus(1);
        when(dataScopeService.canAccessDepartment(2L)).thenReturn(true);
    }

    @Test
    void createReturnsBusinessMessageWhenEmployeeNumberBelongsToDeletedUser() {
        when(sysUserMapper.countByEmployeeNoIncludingDeleted("E0002")).thenReturn(1L);

        Result<?> result = service.create(dto);

        assertEquals(409, result.getCode());
        assertEquals("工号已被使用（包含已删除用户），请更换工号", result.getMsg());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void createRejectsPositionFromAnotherDepartment() {
        when(sysUserMapper.countByEmployeeNoIncludingDeleted("E0002")).thenReturn(0L);
        when(sysUserMapper.countByUsernameIncludingDeleted("new.employee")).thenReturn(0L);
        when(sysDepartmentMapper.selectById(2L)).thenReturn(activeDepartment());
        SysPosition position = new SysPosition();
        position.setPositionId(1L);
        position.setDeptId(3L);
        position.setDeleted(0);
        when(sysPositionMapper.selectById(1L)).thenReturn(position);

        Result<?> result = service.create(dto);

        assertEquals("所选职位不属于当前部门", result.getMsg());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void createStoresEncodedPasswordForValidUser() {
        when(sysUserMapper.countByEmployeeNoIncludingDeleted("E0002")).thenReturn(0L);
        when(sysUserMapper.countByUsernameIncludingDeleted("new.employee")).thenReturn(0L);
        when(sysDepartmentMapper.selectById(2L)).thenReturn(activeDepartment());
        SysPosition position = new SysPosition();
        position.setPositionId(1L);
        position.setDeptId(2L);
        position.setDeleted(0);
        when(sysPositionMapper.selectById(1L)).thenReturn(position);
        when(dataScopeService.canAssignRole(1L)).thenReturn(true);
        when(sysRoleMapper.selectById(1L)).thenReturn(new SysRole().setRoleId(1L));
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);

        Result<?> result = service.create(dto);

        assertEquals("创建成功", result.getMsg());
        verify(sysUserMapper).insert(org.mockito.ArgumentMatchers.argThat(user ->
                "encoded-password".equals(user.getPassword()) && Integer.valueOf(0).equals(user.getDeleted())));
    }

    private SysDepartment activeDepartment() {
        SysDepartment department = new SysDepartment();
        department.setDeptId(2L);
        department.setStatus(1);
        department.setDeleted(0);
        return department;
    }
}
