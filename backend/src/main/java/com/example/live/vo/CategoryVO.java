package com.example.live.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "直播分类")
public record CategoryVO(
        @Schema(description = "分类 ID") Long id,
        @Schema(description = "分类名") String name,
        @Schema(description = "图标标识") String icon,
        @Schema(description = "排序值") Integer sort) {
}
