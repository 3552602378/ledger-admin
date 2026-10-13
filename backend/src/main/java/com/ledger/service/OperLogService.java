package com.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ledger.common.PageResult;
import com.ledger.dto.OperLogQueryDTO;
import com.ledger.entity.SysOperLog;
import com.ledger.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperLogService {

    private final SysOperLogMapper operLogMapper;

    public PageResult<SysOperLog> page(OperLogQueryDTO query) {
        LambdaQueryWrapper<SysOperLog> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.hasText(query.getUsername()), SysOperLog::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getTitle()), SysOperLog::getTitle, query.getTitle())
                .eq(query.getBusinessType() != null, SysOperLog::getBusinessType, query.getBusinessType())
                .eq(query.getStatus() != null, SysOperLog::getStatus, query.getStatus());
        if (StringUtils.hasText(query.getStartDate())) {
            qw.ge(SysOperLog::getCreateTime, LocalDate.parse(query.getStartDate()).atStartOfDay());
        }
        if (StringUtils.hasText(query.getEndDate())) {
            qw.le(SysOperLog::getCreateTime, LocalDate.parse(query.getEndDate()).atTime(23, 59, 59));
        }
        qw.orderByDesc(SysOperLog::getCreateTime).orderByDesc(SysOperLog::getId);

        Page<SysOperLog> page =
                operLogMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}
