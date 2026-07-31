package com.oa.attendance.service;

import com.oa.attendance.entity.Result;
import java.util.Map;

public interface IAgentService {
    Result<Map<String, Object>> process(String message);
}
