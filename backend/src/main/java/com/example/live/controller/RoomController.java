package com.example.live.controller;

import com.example.live.common.result.PageResult;
import com.example.live.common.result.Result;
import com.example.live.service.RoomService;
import com.example.live.vo.LiveRoomVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "直播间")
@Validated
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Operation(summary = "直播间分页列表")
    @GetMapping
    public Result<PageResult<LiveRoomVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为 1") int page,
            @RequestParam(defaultValue = "12") @Min(value = 1, message = "每页至少 1 条")
            @Max(value = 50, message = "每页最多 50 条") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(roomService.pageRooms(page, size, categoryId, keyword));
    }
}
