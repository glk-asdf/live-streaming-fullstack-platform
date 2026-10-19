package com.example.live.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "更新个人资料；字段为 null 表示不修改")
public record UpdateProfileRequest(
        @Size(min = 1, max = 20, message = "昵称需为 1~20 个字符") String nickname,
        @Size(max = 255, message = "头像地址最多 255 个字符") String avatar,
        @Size(max = 255, message = "简介最多 255 个字符") String bio) {
}
