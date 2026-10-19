package com.example.live.common.util;

import com.example.live.common.exception.BizException;
import com.example.live.common.result.ResultCode;
import com.example.live.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 读取当前登录用户。
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long currentUserId() {
        return currentUser().id();
    }

    /**
     * 公开接口可选登录：游客返回 null，不抛 401。
     */
    public static Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.id();
        }
        return null;
    }
}
