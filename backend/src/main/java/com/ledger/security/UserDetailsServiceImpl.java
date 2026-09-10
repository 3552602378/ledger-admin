package com.ledger.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ledger.entity.SysMenu;
import com.ledger.entity.SysRole;
import com.ledger.entity.SysUser;
import com.ledger.entity.SysUserRole;
import com.ledger.mapper.SysMenuMapper;
import com.ledger.mapper.SysRoleMapper;
import com.ledger.mapper.SysUserMapper;
import com.ledger.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        Set<String> permissions = getUserPermissions(user.getId());
        return new LoginUser(user, permissions);
    }

    private Set<String> getUserPermissions(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();

        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        boolean isAdmin = roleIds.stream().anyMatch(roleId -> {
            SysRole role = roleMapper.selectById(roleId);
            return role != null && "admin".equals(role.getRoleCode());
        });

        if (isAdmin) {
            return Collections.singleton("*:*:*");
        }

        return menuMapper.selectPermissionsByRoleIds(roleIds).stream()
                .filter(perm -> perm != null && !perm.isEmpty())
                .collect(Collectors.toSet());
    }
}
