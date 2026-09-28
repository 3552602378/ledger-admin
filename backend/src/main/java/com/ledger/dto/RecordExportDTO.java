package com.ledger.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/** 导出参数：复用收支筛选条件，额外支持按 id 批量导出与全量导出 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RecordExportDTO extends RecordQueryDTO {

    /** 批量导出选中的记录 id 集合，非空时优先级最高 */
    private List<Long> ids;

    /** true 表示导出全量（忽略筛选条件） */
    private Boolean exportAll = false;
}
