package com.ledger.controller;

import com.ledger.common.Result;
import com.ledger.entity.FinCategory;
import com.ledger.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('finance:category:view')")
    public Result<List<FinCategory>> list(@RequestParam(required = false) String type) {
        return Result.ok(categoryService.list(type));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('finance:category:add')")
    public Result<Void> add(@RequestBody FinCategory category) {
        categoryService.add(category);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('finance:category:edit')")
    public Result<Void> edit(@PathVariable Long id, @RequestBody FinCategory category) {
        categoryService.edit(id, category);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('finance:category:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}