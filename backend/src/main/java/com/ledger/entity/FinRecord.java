package com.ledger.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fin_record")
public class FinRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    @NotBlank(message = "收支类型不能为空")
    private String type;

    @NotNull(message = "所属分类不能为空")
    private Long categoryId;

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @NotNull(message = "收支日期不能为空")
    private LocalDate recordDate;

    private String remark;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String categoryName;
}
