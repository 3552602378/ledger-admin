package com.ledger.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/** Excel 导出模型：列顺序与列表展示一致 */
@Data
public class RecordExcelVO {

    @ExcelProperty("日期")
    @ColumnWidth(14)
    private String recordDate;

    @ExcelProperty("类型")
    @ColumnWidth(10)
    private String type;

    @ExcelProperty("分类")
    @ColumnWidth(18)
    private String categoryName;

    @ExcelProperty("金额")
    @ColumnWidth(16)
    @NumberFormat("#,##0.00")
    private BigDecimal amount;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remark;
}
