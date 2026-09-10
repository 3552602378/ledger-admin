package com.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ledger.common.BizException;
import com.ledger.entity.SysMenu;
import com.ledger.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;

    public List<SysMenu> list() {
        return menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getSortOrder));
    }

    public List<Map<String, Object>> tree() {
        List<SysMenu> menus = list();
        return buildTree(menus, 0L);
    }

    private List<Map<String, Object>> buildTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .sorted(Comparator.comparingInt(SysMenu::getSortOrder))
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", m.getId());
                    map.put("parentId", m.getParentId());
                    map.put("menuName", m.getMenuName());
                    map.put("menuType", m.getMenuType());
                    map.put("path", m.getPath());
                    map.put("component", m.getComponent());
                    map.put("perms", m.getPerms());
                    map.put("icon", m.getIcon());
                    map.put("sortOrder", m.getSortOrder());
                    map.put("status", m.getStatus());
                    map.put("children", buildTree(menus, m.getId()));
                    return map;
                }).toList();
    }

    public SysMenu detail(Long id) {
        return menuMapper.selectById(id);
    }

    public void add(SysMenu menu) {
        menuMapper.insert(menu);
    }

    public void edit(Long id, SysMenu menu) {
        SysMenu exist = menuMapper.selectById(id);
        if (exist == null) {
            throw new BizException("菜单不存在");
        }
        menu.setId(id);
        menuMapper.updateById(menu);
    }

    public void delete(Long id) {
        Long count = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (count > 0) {
            throw new BizException("存在子菜单，不能删除");
        }
        menuMapper.deleteById(id);
    }
}
