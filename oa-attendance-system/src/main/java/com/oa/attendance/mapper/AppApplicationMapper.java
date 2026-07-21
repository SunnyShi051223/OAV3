package com.oa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.attendance.entity.AppApplication;
import com.oa.attendance.vo.ApplicationTypeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppApplicationMapper extends BaseMapper<AppApplication> {

    @Select("SELECT dict_value AS value, dict_label AS label " +
            "FROM sys_dict_data WHERE dict_code = 'application_type' AND status = 1 ORDER BY sort_order")
    List<ApplicationTypeVO> selectEnabledApplicationTypes();
}
