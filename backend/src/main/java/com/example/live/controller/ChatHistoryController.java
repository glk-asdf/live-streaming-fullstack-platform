package com.example.live.controller;

import com.example.live.common.result.Result;
import com.example.live.service.ChatService;
import com.example.live.vo.ChatMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "弹幕")
@Validated
@RestController
@RequestMapping("/api/v1/rooms")
public class ChatHistoryController {

    private final ChatService chatService;

    public ChatHistoryController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "最近弹幕历史")
    @GetMapping("/{id}/messages")
    public Result<List<ChatMessageVO>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit) {
        return Result.ok(chatService.history(id, limit));
    }
}
