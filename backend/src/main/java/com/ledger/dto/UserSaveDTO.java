package com.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserSaveDTO {

    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String nickname;

    private String password;

    private Integer status;

    private Long roleId;
}
