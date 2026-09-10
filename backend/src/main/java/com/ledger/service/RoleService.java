package com.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ledger.common.BizException;
import com.ledger.entity.SysRole;
import com.ledger.entity.SysRoleMenu;
import com.ledger.entity.SysUserRole;
import com.ledger.mapper.SysRoleMapper;
import com.ledger.mapper.SysRoleMenuMapper;
import com.ledger.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public List<SysRole> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByDesc(SysRole::getCreateTime));
    }

    public SysRole detail(Long id) {
        return roleMapper.selectById(id);
    }

    public void add(SysRole role) {
        checkRoleCodeUnique(null, role.getRoleCode());
        roleMapper.insert(role);
        saveRoleMenus(role.getId(), role.getMenuIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void edit(Long id, SysRole role) {
        SysRole exist = roleMapper.selectById(id);
        if (exist == null) {
            throw new BizException("角色不存在");
        }
        checkRoleCodeUnique(id, role.getRoleCode());
        exist.setRoleName(role.getRoleName());
        exist.setRoleCode(role.getRoleCode());
        exist.setStatus(role.getStatus());
        exist.setRemark(role.getRemark());
        roleMapper.updateById(exist);
        saveRoleMenus(id, role.getMenuIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
    }

    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    private void checkRoleCodeUnique(Long id, String roleCode) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode);
        if (id != null) {
            wrapper.ne(SysRole::getId, id);
        }
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BizException("角色编码已存在");
        }
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        if (menuIds == null) {
            return;
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }
}
