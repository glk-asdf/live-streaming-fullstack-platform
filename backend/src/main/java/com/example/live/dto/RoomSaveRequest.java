package com.example.live.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "创建/编辑直播间")
public record RoomSaveRequest(
        @NotBlank(message = "直播间标题不能为空")
        @Size(max = 80, message = "标题最多 80 个字符")
        String title,

        @Size(max = 255, message = "封面地址最多 255 个字符")
        String cover,

        @Schema(description = "分类 ID，可为空")
        Long categoryId) {
}
