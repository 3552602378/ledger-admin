package com.ledger.controller;

import com.ledger.common.Result;
import com.ledger.entity.SysMenu;
import com.ledger.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    @PreAuthorize("@ss.hasPermi('system:menu:view')")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(menuService.tree());
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('system:menu:view')")
    public Result<List<SysMenu>> list() {
        return Result.ok(menuService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:menu:view')")
    public Result<SysMenu> detail(@PathVariable Long id) {
        return Result.ok(menuService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    public Result<Void> add(@RequestBody SysMenu menu) {
        menuService.add(menu);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    public Result<Void> edit(@PathVariable Long id, @RequestBody SysMenu menu) {
        menuService.edit(id, menu);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:menu:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
