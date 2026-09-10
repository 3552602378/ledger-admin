package com.ledger.controller;

import com.ledger.common.PageResult;
import com.ledger.common.Result;
import com.ledger.dto.UserQueryDTO;
import com.ledger.dto.UserSaveDTO;
import com.ledger.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('system:user:view')")
    public Result<PageResult<UserVO>> list(UserQueryDTO query) {
        return Result.ok(userService.list(query));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    public Result<Void> add(@Valid @RequestBody UserSaveDTO dto) {
        userService.add(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    public Result<Void> edit(@PathVariable Long id, @Valid @RequestBody UserSaveDTO dto) {
        userService.edit(id, dto);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.changeStatus(id, status);
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("@ss.hasPermi('system:user:reset')")
    public Result<String> resetPassword(@PathVariable Long id) {
        return Result.ok(userService.resetPassword(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:user:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    public record UserVO(Long id, String username, String nickname, Integer status, String createTime) {
    }
}
