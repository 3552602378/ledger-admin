package com.ledger.controller;

import com.ledger.common.Result;
import com.ledger.security.LoginUser;
import com.ledger.security.JwtTokenUtil;
import com.ledger.service.AuthService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        LoginUser loginUser = authService.login(request.getUsername(), request.getPassword());
        String token = jwtTokenUtil.generateToken(loginUser);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", loginUser.getUser());
        return Result.ok(result);
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        return Result.ok(authService.getCurrentUserInfo());
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
