package com.kbsystem.backend.security;

/**
 * 用户上下文工具。
 * 使用 ThreadLocal 在单次请求中保存当前用户信息。
 */
public final class UserContext {

    /**
     * 当前请求用户 ID 容器。
     */
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 当前请求用户名容器。
     */
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 写入当前用户信息。
     *
     * @param userId   用户 ID
     * @param username 用户名
     */
    public static void set(Long userId, String username) {
        USER_ID_HOLDER.set(userId);
        USERNAME_HOLDER.set(username);
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 用户 ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 获取当前用户名。
     *
     * @return 用户名
     */
    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    /**
     * 清理上下文，防止线程复用导致数据污染。
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
    }
}

