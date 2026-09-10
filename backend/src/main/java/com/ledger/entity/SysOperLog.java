package com.ledger.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String title;

    private Integer businessType;

    private String requestMethod;

    private String requestUrl;

    private String requestParams;

    private String responseResult;

    private String ip;

    private Long costTime;

    private Integer status;

    private LocalDateTime createTime;
}
