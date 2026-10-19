package com.example.live.controller;

import com.example.live.common.result.Result;
import com.example.live.service.PlaybackService;
import com.example.live.vo.PlayInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "播放")
@RestController
@RequestMapping("/api/v1/rooms")
public class PlaybackController {

    private final PlaybackService playbackService;

    public PlaybackController(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @Operation(summary = "获取播放地址与 5 分钟播放令牌")
    @GetMapping("/{id}/play-info")
    public Result<PlayInfoVO> playInfo(@PathVariable Long id) {
        return Result.ok(playbackService.playInfo(id));
    }
}
