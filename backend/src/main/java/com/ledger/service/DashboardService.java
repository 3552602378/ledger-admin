package com.ledger.service;

import com.ledger.mapper.FinRecordMapper;
import com.ledger.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FinRecordMapper recordMapper;

    /** 看板统计：近12月/近7日/分类占比，数据隔离 admin 全部、普通用户本人 */
    public Map<String, Object> stats() {
        Long userId = SecurityUtils.isAdmin() ? null : SecurityUtils.getUserId();
        LocalDate today = LocalDate.now();

        // 1. 近 12 个月
        LocalDate monthStart = today.minusMonths(11).withDayOfMonth(1);
        List<String> months = new ArrayList<>();
        List<BigDecimal> monthIncome = new ArrayList<>();
        List<BigDecimal> monthExpense = new ArrayList<>();
        Map<String, Map<String, Object>> mMap = recordMapper.monthly(userId, monthStart, today).stream()
                .collect(Collectors.toMap(r -> (String) r.get("key"), r -> r));
        for (int i = 0; i < 12; i++) {
            String ym = today.minusMonths(11 - i).format(MONTH_FMT);
            months.add(ym);
            Map<String, Object> row = mMap.get(ym);
            monthIncome.add(dec(row == null ? null : row.get("income")));
            monthExpense.add(dec(row == null ? null : row.get("expense")));
        }

        // 2. 近 7 日
        LocalDate dayStart = today.minusDays(6);
        List<String> days = new ArrayList<>();
        List<BigDecimal> dayIncome = new ArrayList<>();
        List<BigDecimal> dayExpense = new ArrayList<>();
        Map<String, Map<String, Object>> dMap = recordMapper.daily(userId, dayStart, today).stream()
                .collect(Collectors.toMap(r -> (String) r.get("key"), r -> r));
        for (int i = 0; i < 7; i++) {
            String d = today.minusDays(6 - i).format(DAY_FMT);
            days.add(d.substring(5));
            Map<String, Object> row = dMap.get(d);
            dayIncome.add(dec(row == null ? null : row.get("income")));
            dayExpense.add(dec(row == null ? null : row.get("expense")));
        }

        // 3. 分类金额占比
        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map<String, Object> r : recordMapper.sumByCategory(userId)) {
            Map<String, Object> c = new HashMap<>();
            c.put("name", r.get("category"));
            c.put("type", r.get("ctype"));
            c.put("amount", dec(r.get("amount")));
            categories.add(c);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("months", months);
        result.put("monthIncome", monthIncome);
        result.put("monthExpense", monthExpense);
        result.put("days", days);
        result.put("dayIncome", dayIncome);
        result.put("dayExpense", dayExpense);
        result.put("categories", categories);
        return result;
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString()).setScale(2);
    }
}