package com.ledger.dto;

import lombok.Data;

@Data
public class OperLogQueryDTO {
    /** 页码 */
    private long pageNum = 1;
    /** 每页条数 */
    private long pageSize = 10;
    /** 操作人（模糊） */
    private String username;
    /** 模块标题（模糊） */
    private String title;
    /** 业务类型 1新增 2修改 3删除 4导出 5登录 */
    private Integer businessType;
    /** 状态 1成功 0失败 */
    private Integer status;
    /** 时间范围起点（yyyy-MM-dd） */
    private String startDate;
    /** 时间范围终点（yyyy-MM-dd） */
    private String endDate;
}
