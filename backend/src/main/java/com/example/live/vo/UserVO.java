package com.example.live.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "用户资料（不含密码）")
public record UserVO(
        @Schema(description = "用户 ID") Long id,
        @Schema(description = "登录名") String username,
        @Schema(description = "昵称") String nickname,
        @Schema(description = "头像地址") String avatar,
        @Schema(description = "个人简介") String bio,
        @Schema(description = "角色：USER / ANCHOR / ADMIN") String role,
        @Schema(description = "状态：0 正常 / 1 禁言 / 2 封禁") Integer status,
        @Schema(description = "注册时间") LocalDateTime createdAt) {
}
