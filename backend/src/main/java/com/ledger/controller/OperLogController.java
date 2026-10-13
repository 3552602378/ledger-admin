package com.ledger.controller;

import com.ledger.common.PageResult;
import com.ledger.common.Result;
import com.ledger.dto.OperLogQueryDTO;
import com.ledger.entity.SysOperLog;
import com.ledger.service.OperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/logs")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogService operLogService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('audit:log:view')")
    public Result<PageResult<SysOperLog>> page(OperLogQueryDTO query) {
        return Result.ok(operLogService.page(query));
    }
}
