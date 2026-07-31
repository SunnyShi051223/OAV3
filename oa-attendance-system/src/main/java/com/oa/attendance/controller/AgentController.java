package com.oa.attendance.controller;

import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin
public class AgentController {

    @Autowired
    private IAgentService agentService;

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "").trim();
        if (message.isEmpty()) {
            return Result.error("请输入您想做的事情");
        }
        return agentService.process(message);
    }
}
