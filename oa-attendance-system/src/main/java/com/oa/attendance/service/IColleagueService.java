package com.oa.attendance.service;

import com.oa.attendance.entity.Result;
import com.oa.attendance.vo.ColleagueVO;

import java.util.List;

public interface IColleagueService {

    /**
     * 搜索同事（根据当前用户角色进行数据范围过滤）
     */
    Result<List<ColleagueVO>> search(String keyword);
}
