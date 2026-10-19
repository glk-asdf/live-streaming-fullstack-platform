package com.example.live.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 对象存储配置。
 */
@ConfigurationProperties(prefix = "live.minio")
public record MinioProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String publicBase) {
}
