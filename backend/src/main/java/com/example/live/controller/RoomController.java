package com.example.live.controller;

import com.example.live.common.result.PageResult;
import com.example.live.common.result.Result;
import com.example.live.dto.RoomSaveRequest;
import com.example.live.service.RoomService;
import com.example.live.vo.LiveRoomVO;
import com.example.live.vo.RoomStreamVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(roomService.pageRooms(page, size, categoryId, keyword, status));
    }

    @Operation(summary = "我的直播间（无房间时 data 为空）")
    @GetMapping("/mine")
    public Result<LiveRoomVO> mine() {
        return Result.ok(roomService.myRoom());
    }

    @Operation(summary = "直播间详情")
    @GetMapping("/{id}")
    public Result<LiveRoomVO> detail(@PathVariable Long id) {
        return Result.ok(roomService.detail(id));
    }

    @Operation(summary = "创建直播间")
    @PostMapping
    public Result<LiveRoomVO> create(@Valid @RequestBody RoomSaveRequest request) {
        return Result.ok(roomService.create(request));
    }

    @Operation(summary = "编辑直播间")
    @PutMapping("/{id}")
    public Result<LiveRoomVO> update(@PathVariable Long id, @Valid @RequestBody RoomSaveRequest request) {
        return Result.ok(roomService.update(id, request));
    }

    @Operation(summary = "准备开播：轮换推流密钥并返回 OBS 参数")
    @PostMapping("/{id}/start")
    public Result<RoomStreamVO> start(@PathVariable Long id) {
        return Result.ok(roomService.start(id));
    }

    @Operation(summary = "重置推流密钥")
    @PostMapping("/{id}/stream-key/reset")
    public Result<RoomStreamVO> resetStreamKey(@PathVariable Long id) {
        return Result.ok(roomService.resetStreamKey(id));
    }

    @Operation(summary = "查看推流参数（仅房主）")
    @GetMapping("/{id}/stream")
    public Result<RoomStreamVO> streamInfo(@PathVariable Long id) {
        return Result.ok(roomService.streamInfo(id));
    }

    @Operation(summary = "结束直播")
    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id) {
        roomService.stop(id);
        return Result.ok();
    }
}
