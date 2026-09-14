package com.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ledger.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

//继承`BaseMapper`：
// 自带 MyBatis-Plus 基础 CRUD（`selectById`、`insert`、`updateById`、`selectList`、`selectCount`）
// **不需要写 xml**
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    List<String> selectPermissionsByRoleIds(@Param("roleIds") List<Long> roleIds);
}
