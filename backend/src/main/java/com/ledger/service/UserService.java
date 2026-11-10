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

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();

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
        List<Long> userIds = result.getRecords().stream().map(SysUser::getId).toList();
        Map<Long, Long> roleMap = userIds.isEmpty() ? Map.of() : userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds))
                .stream().collect(Collectors.toMap(SysUserRole::getUserId, SysUserRole::getRoleId, (a, b) -> a));
        List<UserController.UserVO> rows = result.getRecords().stream().map(u ->
                new UserController.UserVO(u.getId(), u.getUsername(), u.getNickname(), u.getStatus(),
                        roleMap.get(u.getId()), u.getCreateTime().toString())
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
        String newPassword = randomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return newPassword;
    }

    /** 生成包含大小写字母与数字的 8 位随机密码，避免使用固定默认密码 */
    private String randomPassword() {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
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
