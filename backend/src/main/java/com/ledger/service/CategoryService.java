package com.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ledger.common.BizException;
import com.ledger.entity.FinCategory;
import com.ledger.entity.FinRecord;
import com.ledger.mapper.FinCategoryMapper;
import com.ledger.mapper.FinRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final FinCategoryMapper categoryMapper;
    private final FinRecordMapper recordMapper;

    public List<FinCategory> list(String type) {
        LambdaQueryWrapper<FinCategory> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            wrapper.eq(FinCategory::getType, type);
        }
        wrapper.orderByAsc(FinCategory::getType).orderByAsc(FinCategory::getId);
        return categoryMapper.selectList(wrapper);
    }

    public void add(FinCategory category) {
        checkNameUnique(null, category.getType(), category.getName());
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        categoryMapper.insert(category);
    }

    public void edit(Long id, FinCategory category) {
        FinCategory exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BizException("分类不存在");
        }
        checkNameUnique(id, category.getType(), category.getName());
        exist.setType(category.getType());
        exist.setName(category.getName());
        exist.setStatus(category.getStatus());
        categoryMapper.updateById(exist);
    }

    public void delete(Long id) {
        // 删除前校验是否被收支记录引用
        Long count = recordMapper.selectCount(
                new LambdaQueryWrapper<FinRecord>().eq(FinRecord::getCategoryId, id));
        if (count != null && count > 0) {
            throw new BizException("该分类已被收支记录引用，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    private void checkNameUnique(Long id, String type, String name) {
        LambdaQueryWrapper<FinCategory> wrapper = new LambdaQueryWrapper<FinCategory>()
                .eq(FinCategory::getType, type)
                .eq(FinCategory::getName, name);
        if (id != null) {
            wrapper.ne(FinCategory::getId, id);
        }
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BizException("该类型下分类名称已存在");
        }
    }
}