package com.example.live.common.result;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 业务码与 HTTP 状态码的对应关系。
 */
@Schema(description = "业务响应码")
public enum ResultCode {

    SUCCESS(0, 200, "ok"),
    BAD_REQUEST(40000, 400, "请求参数不合法"),
    UNAUTHORIZED(40100, 401, "未登录或登录已过期"),
    FORBIDDEN(40300, 403, "没有权限执行该操作"),
    NOT_FOUND(40400, 404, "资源不存在"),
    METHOD_NOT_ALLOWED(40500, 405, "请求方法不支持"),
    CONFLICT(40900, 409, "资源状态冲突"),
    UNSUPPORTED_MEDIA_TYPE(41500, 415, "请求内容类型不支持"),
    PAYLOAD_TOO_LARGE(41300, 413, "上传文件过大"),
    TOO_MANY_REQUESTS(42900, 429, "操作过于频繁"),
    INTERNAL_ERROR(50000, 500, "服务器内部错误");

    private final int code;
    private final int httpStatus;
    private final String message;

    ResultCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
