package com.example.live.controller;

import com.example.live.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 链路探活接口，供前端首页与部署冒烟测试使用。
 */
@Tag(name = "探活")
@RestController
@RequestMapping("/api/v1")
public class PingController {

    @Value("${spring.application.name:live-backend}")
    private String applicationName;

    @Operation(summary = "服务探活")
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        return Result.ok(Map.of(
                "service", applicationName,
                "version", "0.1.0",
                "serverTime", LocalDateTime.now()
        ));
    }
}
