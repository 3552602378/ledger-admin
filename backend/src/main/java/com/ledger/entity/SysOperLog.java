package com.ledger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 操作日志 */
@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 操作人 */
    private String username;
    /** 模块标题 */
    private String title;
    /** 业务类型 1新增 2修改 3删除 4导出 5登录 */
    private Integer businessType;
    /** 请求方法 GET/POST/PUT/DELETE */
    private String requestMethod;
    /** 请求 URL */
    private String requestUrl;
    /** 请求参数（JSON，前 512 字符，密码已脱敏） */
    private String requestParams;
    /** 错误信息（失败时记录） */
    private String responseResult;
    /** 操作 IP */
    private String ip;
    /** 耗时 ms */
    private Long costTime;
    /** 状态 1成功 0失败 */
    private Integer status;
    private LocalDateTime createTime;
}
