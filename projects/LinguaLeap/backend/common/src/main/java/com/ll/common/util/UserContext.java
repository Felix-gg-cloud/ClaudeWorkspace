package com.ll.common.util;

/**
 * ThreadLocal holder for current user ID, set by Gateway filter and read by downstream services.
 */
public final class UserContext {

    private UserContext() {}

    public static final String HEADER_USER_ID = "X-User-Id";

    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        CURRENT_USER.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
