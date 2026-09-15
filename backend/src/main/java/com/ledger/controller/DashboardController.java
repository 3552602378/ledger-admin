package com.ledger.controller;

import com.ledger.common.Result;
import com.ledger.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("@ss.hasPermi('dashboard:view')")
    public Result<Map<String, Object>> stats() {
        return Result.ok(dashboardService.stats());
    }
}