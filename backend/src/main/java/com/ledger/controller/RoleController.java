package com.ledger.controller;

import com.ledger.common.Result;
import com.ledger.entity.SysRole;
import com.ledger.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('system:role:view')")
    public Result<List<SysRole>> list() {
        return Result.ok(roleService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:role:view')")
    public Result<SysRole> detail(@PathVariable Long id) {
        return Result.ok(roleService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    public Result<Void> add(@RequestBody SysRole role) {
        roleService.add(role);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    public Result<Void> edit(@PathVariable Long id, @RequestBody SysRole role) {
        roleService.edit(id, role);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:role:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("@ss.hasPermi('system:role:view')")
    public Result<List<Long>> roleMenus(@PathVariable Long id) {
        return Result.ok(roleService.getRoleMenuIds(id));
    }
}
