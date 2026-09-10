package com.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ledger.common.BizException;
import com.ledger.common.PageResult;
import com.ledger.controller.UserController;
import com.ledger.dto.UserQueryDTO;
import com.ledger.dto.UserSaveDTO;
import com.ledger.entity.SysUser;
import com.ledger.entity.SysUserRole;
import com.ledger.mapper.SysUserMapper;
import com.ledger.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public PageResult<UserController.UserVO> list(UserQueryDTO query) {
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreateTime);
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(SysUser::getUsername, query.getKeyword())
                    .or()
                    .like(SysUser::getNickname, query.getKeyword()));
        }
        Page<SysUser> result = userMapper.selectPage(page, wrapper);
        List<UserController.UserVO> rows = result.getRecords().stream().map(u ->
                new UserController.UserVO(u.getId(), u.getUsername(), u.getNickname(), u.getStatus(), u.getCreateTime().toString())
        ).toList();
        return new PageResult<>(result.getTotal(), rows);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(UserSaveDTO dto) {
        checkUsernameUnique(null, dto.getUsername());
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : "123456"));
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        userMapper.insert(user);
        saveUserRole(user.getId(), dto.getRoleId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void edit(Long id, UserSaveDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        checkUsernameUnique(id, dto.getUsername());
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setStatus(dto.getStatus());
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userMapper.updateById(user);
        saveUserRole(id, dto.getRoleId());
    }

    public void changeStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public String resetPassword(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        String newPassword = "123456";
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return newPassword;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    private void checkUsernameUnique(Long id, String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        if (id != null) {
            wrapper.ne(SysUser::getId, id);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException("用户名已存在");
        }
    }

    private void saveUserRole(Long userId, Long roleId) {
        if (roleId == null) {
            return;
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }
}
