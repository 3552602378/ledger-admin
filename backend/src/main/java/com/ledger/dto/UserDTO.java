package com.ledger.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {

    private Long id;

    private String username;

    private String nickname;

    private Integer status;

    private LocalDateTime createTime;
}
