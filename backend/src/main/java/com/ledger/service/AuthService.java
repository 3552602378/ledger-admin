package com.ledger.service;

import com.ledger.common.BizException;
import com.ledger.entity.SysMenu;
import com.ledger.mapper.SysMenuMapper;
import com.ledger.mapper.SysRoleMenuMapper;
import com.ledger.mapper.SysUserMapper;
import com.ledger.mapper.SysUserRoleMapper;
import com.ledger.security.LoginUser;
import com.ledger.security.SecurityUtils;
import com.ledger.entity.SysRoleMenu;
import com.ledger.entity.SysUser;
import com.ledger.entity.SysUserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public LoginUser login(String username, String password) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        return (LoginUser) authentication.getPrincipal();
    }

    public Map<String, Object> getCurrentUserInfo() {
        LoginUser loginUser = Optional.ofNullable(SecurityUtils.getLoginUser())
                .orElseThrow(() -> new BizException("未登录"));

        Long userId = loginUser.getUser().getId();
        List<Long> roleIds = userRoleMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                                .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();

        List<SysMenu> allMenus;
        if (loginUser.isAdmin()) {
            allMenus = menuMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysMenu>()
                            .eq(SysMenu::getStatus, 1)
                            .orderByAsc(SysMenu::getSortOrder));
        } else {
            List<Long> menuIds = roleMenuMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRoleMenu>()
                                    .in(SysRoleMenu::getRoleId, roleIds))
                    .stream().map(SysRoleMenu::getMenuId).distinct().toList();
            allMenus = menuIds.isEmpty() ? new ArrayList<>() : menuMapper.selectBatchIds(menuIds);
        }

        List<SysMenu> menus = allMenus.stream()
                .filter(m -> m.getMenuType() != 3)
                .sorted(Comparator.comparingInt(SysMenu::getSortOrder))
                .toList();

        List<String> perms = allMenus.stream()
                .filter(m -> m.getPerms() != null && !m.getPerms().isEmpty())
                .map(SysMenu::getPerms)
                .distinct()
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("user", loginUser.getUser());
        result.put("menus", buildMenuTree(menus, 0L));
        result.put("perms", perms);
        result.put("admin", loginUser.isAdmin());
        return result;
    }

    private List<Map<String, Object>> buildMenuTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", m.getId());
                    map.put("menuName", m.getMenuName());
                    map.put("path", m.getPath());
                    map.put("component", m.getComponent());
                    map.put("icon", m.getIcon());
                    map.put("menuType", m.getMenuType());
                    map.put("perms", m.getPerms());
                    map.put("children", buildMenuTree(menus, m.getId()));
                    return map;
                }).toList();
    }
}
