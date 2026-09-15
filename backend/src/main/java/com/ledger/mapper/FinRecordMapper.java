package com.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ledger.entity.FinRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface FinRecordMapper extends BaseMapper<FinRecord> {

    /** 按自然月汇总收入/支出，用于看板近 12 个月折线图 */
    @Select("""
            <script>
            SELECT DATE_FORMAT(record_date, '%Y-%m') AS `key`,
                   SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END) AS income,
                   SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END) AS expense
            FROM fin_record
            WHERE is_deleted = 0 AND record_date >= #{start} AND record_date &lt;= #{end}
              <if test="userId != null">AND user_id = #{userId}</if>
            GROUP BY `key`
            </script>
            """)
    List<Map<String, Object>> monthly(@Param("userId") Long userId,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);

    /** 按天汇总收入/支出，用于看板近 7 日折线图 */
    @Select("""
            <script>
            SELECT DATE_FORMAT(record_date, '%Y-%m-%d') AS `key`,
                   SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END) AS income,
                   SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END) AS expense
            FROM fin_record
            WHERE is_deleted = 0 AND record_date >= #{start} AND record_date &lt;= #{end}
              <if test="userId != null">AND user_id = #{userId}</if>
            GROUP BY `key`
            </script>
            """)
    List<Map<String, Object>> daily(@Param("userId") Long userId,
                                    @Param("start") LocalDate start,
                                    @Param("end") LocalDate end);

    /** 分类金额占比，用于看板饼图 */
    @Select("""
            <script>
            SELECT c.name AS category, c.type AS ctype, SUM(r.amount) AS amount
            FROM fin_record r
            JOIN fin_category c ON r.category_id = c.id
            WHERE r.is_deleted = 0
              <if test="userId != null">AND r.user_id = #{userId}</if>
            GROUP BY c.id, c.name, c.type
            </script>
            """)
    List<Map<String, Object>> sumByCategory(@Param("userId") Long userId);
}
