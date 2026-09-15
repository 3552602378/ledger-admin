package com.ledger.controller;

import com.ledger.common.PageResult;
import com.ledger.common.Result;
import com.ledger.dto.RecordQueryDTO;
import com.ledger.entity.FinRecord;
import com.ledger.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/finance/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('finance:record:view')")
    public Result<PageResult<FinRecord>> page(RecordQueryDTO query) {
        return Result.ok(recordService.page(query));
    }

    @GetMapping("/statistics")
    @PreAuthorize("@ss.hasPermi('finance:record:view')")
    public Result<Map<String, Object>> statistics(RecordQueryDTO query) {
        return Result.ok(recordService.statistics(query));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('finance:record:add')")
    public Result<Void> add(@RequestBody FinRecord record) {
        recordService.add(record);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('finance:record:edit')")
    public Result<Void> edit(@PathVariable Long id, @RequestBody FinRecord record) {
        recordService.edit(id, record);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('finance:record:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        recordService.delete(id);
        return Result.ok();
    }
}