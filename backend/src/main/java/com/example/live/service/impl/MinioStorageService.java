package com.example.live.service.impl;

import com.example.live.common.exception.BizException;
import com.example.live.common.result.ResultCode;
import com.example.live.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);
    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/gif");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp",
            "image/gif", "gif");

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private volatile boolean bucketReady;

    public MinioStorageService(MinioProperties properties) {
        this.properties = properties;
        this.minioClient = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @PostConstruct
    public void init() {
        try {
            ensureBucket();
        } catch (Exception ex) {
            // MinIO 未启动时不阻塞整个应用，上传时再给出明确错误
            log.warn("MinIO init skipped: {}", ex.getMessage());
        }
    }

    public String uploadAvatar(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择头像文件");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BizException(ResultCode.PAYLOAD_TOO_LARGE, "头像不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BizException(ResultCode.UNSUPPORTED_MEDIA_TYPE, "仅支持 PNG/JPEG/WebP/GIF 图片");
        }

        String extension = EXTENSIONS.get(contentType);
        String objectName = "avatars/" + userId + "/" + UUID.randomUUID() + "." + extension;
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build());
            return trimSlash(properties.publicBase()) + "/" + objectName;
        } catch (Exception ex) {
            log.warn("avatar upload failed, userId={}", userId, ex);
            throw new BizException(ResultCode.INTERNAL_ERROR, "头像上传失败，请检查 MinIO 服务");
        }
    }

    private synchronized void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(properties.bucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
        }
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::%s/*"]
                  }]
                }
                """.formatted(properties.bucket());
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(properties.bucket())
                .config(policy)
                .build());
        bucketReady = true;
        log.info("MinIO bucket ready: {}", properties.bucket());
    }

    private String trimSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
