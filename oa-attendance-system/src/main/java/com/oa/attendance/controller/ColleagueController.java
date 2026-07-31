package com.oa.attendance.controller;

import com.oa.attendance.entity.Result;
import com.oa.attendance.service.IColleagueService;
import com.oa.attendance.vo.ColleagueVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colleagues")
@Validated
@CrossOrigin
public class ColleagueController {

    @Autowired
    private IColleagueService colleagueService;

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('colleague:query')")
    public Result<List<ColleagueVO>> search(@RequestParam(required = false, defaultValue = "") String keyword) {
        return colleagueService.search(keyword);
    }
}
