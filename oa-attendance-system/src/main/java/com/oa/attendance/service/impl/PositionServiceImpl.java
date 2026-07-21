package com.oa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oa.attendance.dto.PositionCreateDTO;
import com.oa.attendance.dto.PositionUpdateDTO;
import com.oa.attendance.entity.Result;
import com.oa.attendance.entity.SysDepartment;
import com.oa.attendance.entity.SysPosition;
import com.oa.attendance.entity.SysUser;
import com.oa.attendance.exception.BusinessException;
import com.oa.attendance.mapper.SysDepartmentMapper;
import com.oa.attendance.mapper.SysPositionMapper;
import com.oa.attendance.mapper.SysUserMapper;
import com.oa.attendance.service.DataScopeService;
import com.oa.attendance.service.IPositionService;
import com.oa.attendance.vo.PositionListVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 职位服务实现类
 */
@Service
public class PositionServiceImpl implements IPositionService {

    @Autowired
    private SysPositionMapper positionMapper;

    @Autowired
    private SysDepartmentMapper departmentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Override
    @Transactional
    public Result<?> create(PositionCreateDTO dto) {
        if (!dataScopeService.canAccessDepartment(dto.getDeptId())) {
            return Result.error("只能维护本部门职位");
        }

        // 检查部门是否存在
        SysDepartment department = departmentMapper.selectById(dto.getDeptId());
        if (department == null) {
            return Result.error("部门不存在");
        }

        // 检查职位名称是否已存在
        QueryWrapper<SysPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("position_name", dto.getPositionName());
        wrapper.eq("dept_id", dto.getDeptId());
        wrapper.eq("deleted", 0);
        Long count = positionMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("该部门下职位名称已存在");
        }

        SysPosition position = new SysPosition();
        BeanUtils.copyProperties(dto, position);
        position.setDeleted(0);
        position.setCreateTime(LocalDateTime.now());
        position.setUpdateTime(LocalDateTime.now());

        int result = positionMapper.insert(position);
        if (result > 0) {
            return Result.success("创建成功");
        }
        return Result.error("创建失败");
    }

    @Override
    @Transactional
    public Result<?> update(PositionUpdateDTO dto) {
        SysPosition existing = positionMapper.selectById(dto.getPositionId());
        if (existing != null && (!dataScopeService.canAccessDepartment(existing.getDeptId())
                || !dataScopeService.canAccessDepartment(dto.getDeptId()))) {
            return Result.error("只能维护本部门职位");
        }
        if (existing == null) {
            return Result.error("职位不存在");
        }

        // 检查部门是否存在
        SysDepartment department = departmentMapper.selectById(dto.getDeptId());
        if (department == null) {
            return Result.error("部门不存在");
        }

        // 检查职位名称是否与其他职位冲突（排除当前职位）
        QueryWrapper<SysPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("position_name", dto.getPositionName());
        wrapper.eq("dept_id", dto.getDeptId());
        wrapper.eq("deleted", 0);
        wrapper.ne("position_id", dto.getPositionId());
        Long count = positionMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("该部门下职位名称已存在");
        }

        SysPosition position = new SysPosition();
        BeanUtils.copyProperties(dto, position);
        position.setUpdateTime(LocalDateTime.now());

        int result = positionMapper.updateById(position);
        if (result > 0) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    @Override
    @Transactional
    public Result<?> delete(Long positionId) {
        SysPosition position = positionMapper.selectById(positionId);
        if (position != null && !dataScopeService.canAccessDepartment(position.getDeptId())) {
            return Result.error("只能删除本部门职位");
        }

        if (position == null) {
            throw new BusinessException("职位不存在");
        }

        // 检查是否有用户使用该职位
        QueryWrapper<SysUser> userWrapper = new QueryWrapper<>();
        userWrapper.eq("position_id", positionId);
        userWrapper.eq("deleted", 0);
        Long userCount = sysUserMapper.selectCount(userWrapper);
        if (userCount > 0) {
            return Result.error("该职位下存在用户，无法删除");
        }

        int result = positionMapper.softDeleteById(positionId);
        if (result > 0) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    @Override
    public Result<PositionListVO> getById(Long id) {
        SysPosition position = positionMapper.selectById(id);
        if (position != null && position.getDeleted() == 0) {
            if (!dataScopeService.canAccessDepartment(position.getDeptId())) {
                return Result.error("只能查看本部门职位");
            }
            PositionListVO vo = new PositionListVO();
            BeanUtils.copyProperties(position, vo);

            // 查询部门名称
            if (position.getDeptId() != null) {
                SysDepartment department = departmentMapper.selectById(position.getDeptId());
                if (department != null) {
                    vo.setDeptName(department.getDeptName());
                }
            }

            return Result.success("查询成功", vo);
        }
        return Result.error("职位不存在");
    }

    @Override
    public Result<List<PositionListVO>> listAll() {
        QueryWrapper<SysPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        if (dataScopeService.hasDepartmentDataAccess()) {
            wrapper.eq("dept_id", dataScopeService.getCurrentDeptId());
        }
        wrapper.orderByDesc("create_time");

        List<SysPosition> positions = positionMapper.selectList(wrapper);
        List<PositionListVO> vos = positions.stream().map(position -> {
            PositionListVO vo = new PositionListVO();
            BeanUtils.copyProperties(position, vo);

            // 查询部门名称
            if (position.getDeptId() != null) {
                SysDepartment department = departmentMapper.selectById(position.getDeptId());
                if (department != null) {
                    vo.setDeptName(department.getDeptName());
                }
            }

            return vo;
        }).collect(Collectors.toList());

        return Result.success("查询成功", vos);
    }

    @Override
    public Result<List<PositionListVO>> listByDeptId(Long deptId) {
        if (!dataScopeService.canAccessDepartment(deptId)) {
            return Result.error("只能查看本部门职位");
        }

        SysDepartment department = departmentMapper.selectById(deptId);
        if (department == null) {
            return Result.error("部门不存在");
        }

        QueryWrapper<SysPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("dept_id", deptId);
        wrapper.eq("deleted", 0);
        List<SysPosition> positions = positionMapper.selectList(wrapper);

        List<PositionListVO> vos = positions.stream().map(position -> {
            PositionListVO vo = new PositionListVO();
            BeanUtils.copyProperties(position, vo);

            // 查询部门名称
            vo.setDeptName(department.getDeptName());

            return vo;
        }).collect(Collectors.toList());

        return Result.success("查询成功", vos);
    }
}
