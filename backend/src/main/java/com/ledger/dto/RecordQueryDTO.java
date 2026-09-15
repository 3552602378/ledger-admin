package com.ledger.dto;

import lombok.Data;

@Data
public class RecordQueryDTO {
    /** 页码 */
    private long pageNum = 1;
    /** 每页条数 */
    private long pageSize = 10;
    /** 时间维度：today / week / month / custom */
    private String dateType;
    /** custom 时间范围起点（yyyy-MM-dd） */
    private String startDate;
    /** custom 时间范围终点（yyyy-MM-dd） */
    private String endDate;
    /** 收支类型：income / expense */
    private String type;
    /** 分类 ID */
    private Long categoryId;
    /** 备注模糊搜索 */
    private String remark;
}