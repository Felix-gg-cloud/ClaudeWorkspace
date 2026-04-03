package com.ll.ai.service;

import com.ll.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内存滑动窗口限流器
 */
@Component
public class RateLimiter {

    private static final int USER_LIMIT_PER_MINUTE = 10;
    private static final int GLOBAL_LIMIT_PER_MINUTE = 50;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<Long, WindowCounter> userCounters = new ConcurrentHashMap<>();
    private final WindowCounter globalCounter = new WindowCounter();

    /**
     * 检查限流，超限则抛异常
     */
    public void check(Long userId) {
        // 全局限流
        if (globalCounter.getCount() >= GLOBAL_LIMIT_PER_MINUTE) {
            throw new BizException(429, "AI 正忙，请稍后再试");
        }

        // 用户限流
        if (userId != null) {
            WindowCounter userCounter = userCounters.computeIfAbsent(userId, k -> new WindowCounter());
            if (userCounter.getCount() >= USER_LIMIT_PER_MINUTE) {
                throw new BizException(429, "请求过于频繁，请稍后再试");
            }
        }
    }

    /**
     * 记录一次调用
     */
    public void record(Long userId) {
        globalCounter.increment();
        if (userId != null) {
            userCounters.computeIfAbsent(userId, k -> new WindowCounter()).increment();
        }
    }

    /**
     * 滑动窗口计数器
     */
    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        int getCount() {
            resetIfExpired();
            return count.get();
        }

        void increment() {
            resetIfExpired();
            count.incrementAndGet();
        }

        private void resetIfExpired() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                synchronized (this) {
                    if (now - windowStart > WINDOW_MS) {
                        count.set(0);
                        windowStart = now;
                    }
                }
            }
        }
    }
}
