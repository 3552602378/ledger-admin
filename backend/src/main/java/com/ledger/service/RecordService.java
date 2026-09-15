package com.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ledger.common.BizException;
import com.ledger.common.PageResult;
import com.ledger.dto.RecordQueryDTO;
import com.ledger.entity.FinCategory;
import com.ledger.entity.FinRecord;
import com.ledger.mapper.FinCategoryMapper;
import com.ledger.mapper.FinRecordMapper;
import com.ledger.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecordService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FinRecordMapper recordMapper;
    private final FinCategoryMapper categoryMapper;

    public PageResult<FinRecord> page(RecordQueryDTO query) {
        Long userId = SecurityUtils.getUserId();
        boolean admin = SecurityUtils.isAdmin();

        LambdaQueryWrapper<FinRecord> wrapper = new LambdaQueryWrapper<>();
        // 权限隔离：普通用户只能查看本人记录，管理员可查看全部
        if (!admin) {
            wrapper.eq(FinRecord::getUserId, userId);
        }

        // 时间维度筛选
        if (query.getDateType() != null && !query.getDateType().isBlank()) {
            LocalDate today = LocalDate.now();
            LocalDate start = null;
            LocalDate end = null;
            switch (query.getDateType()) {
                case "today" -> start = today;
                case "week" -> { start = today.minusDays(6); end = today; }
                case "month" -> { start = today.withDayOfMonth(1); end = today; }
                case "custom" -> {
                    if (query.getStartDate() != null && !query.getStartDate().isBlank()) {
                        start = LocalDate.parse(query.getStartDate(), DAY_FMT);
                    }
                    if (query.getEndDate() != null && !query.getEndDate().isBlank()) {
                        end = LocalDate.parse(query.getEndDate(), DAY_FMT);
                    }
                }
                default -> { } // 忽略未知维度
            }
            if (start != null && end != null && start.equals(end)) {
                wrapper.eq(FinRecord::getRecordDate, start);
            } else {
                if (start != null) wrapper.ge(FinRecord::getRecordDate, start);
                if (end != null) wrapper.le(FinRecord::getRecordDate, end);
            }
        }

        // 类型 / 分类 / 备注模糊
        if (query.getType() != null && !query.getType().isBlank()) {
            wrapper.eq(FinRecord::getType, query.getType());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(FinRecord::getCategoryId, query.getCategoryId());
        }
        if (query.getRemark() != null && !query.getRemark().isBlank()) {
            wrapper.like(FinRecord::getRemark, query.getRemark());
        }

        wrapper.orderByDesc(FinRecord::getRecordDate).orderByDesc(FinRecord::getId);

        Page<FinRecord> page = recordMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<FinRecord> rows = page.getRecords();
        if (!rows.isEmpty()) {
            Map<Long, String> nameMap = categoryMapper.selectList(
                            new LambdaQueryWrapper<FinCategory>()
                                    .in(FinCategory::getId, rows.stream().map(FinRecord::getCategoryId).distinct().collect(Collectors.toList())))
                    .stream().collect(Collectors.toMap(FinCategory::getId, FinCategory::getName, (a, b) -> a));
            rows.forEach(r -> r.setCategoryName(nameMap.get(r.getCategoryId())));
        }
        return new PageResult<>(page.getTotal(), rows);
    }

    public void add(FinRecord record) {
        record.setUserId(SecurityUtils.getUserId());
        recordMapper.insert(record);
    }

    public void edit(Long id, FinRecord record) {
        FinRecord exist = getOwnedRecord(id);
        exist.setType(record.getType());
        exist.setCategoryId(record.getCategoryId());
        exist.setAmount(record.getAmount());
        exist.setRecordDate(record.getRecordDate());
        exist.setRemark(record.getRemark());
        recordMapper.updateById(exist);
    }

    public void delete(Long id) {
        FinRecord exist = getOwnedRecord(id);
        recordMapper.deleteById(exist.getId());
    }

    /** 校验记录存在且归属当前用户（非管理员只能操作本人记录） */
    private FinRecord getOwnedRecord(Long id) {
        FinRecord exist = recordMapper.selectById(id);
        if (exist == null) {
            throw new BizException("收支记录不存在");
        }
        boolean admin = SecurityUtils.isAdmin();
        if (!admin && !exist.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BizException("无权操作他人的收支记录");
        }
        return exist;
    }
}