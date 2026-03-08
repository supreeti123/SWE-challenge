package com.todoservice.web;

import com.todoservice.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * In-memory fixed-window rate limiter for write operations.
 */
@Component
public class WriteRateLimitInterceptor implements HandlerInterceptor {

    private final int maxRequests;
    private final long windowSeconds;
    private final Clock clock;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public WriteRateLimitInterceptor(
            @Value("${app.rate-limit.write.max-requests:120}") int maxRequests,
            @Value("${app.rate-limit.write.window-seconds:60}") long windowSeconds,
            Clock clock) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!isWriteRequest(request) || !isTodoEndpoint(request)) {
            return true;
        }

        long now = Instant.now(clock).getEpochSecond();
        String key = resolveClientIp(request);
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(now));

        synchronized (counter) {
            long elapsed = now - counter.windowStartEpochSeconds;
            if (elapsed >= windowSeconds) {
                counter.windowStartEpochSeconds = now;
                counter.count.set(0);
            }

            if (counter.count.get() >= maxRequests) {
                long retryAfter = Math.max(1, windowSeconds - (now - counter.windowStartEpochSeconds));
                throw new RateLimitExceededException(retryAfter);
            }

            counter.count.incrementAndGet();
            return true;
        }
    }

    private boolean isWriteRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PATCH".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
    }

    private boolean isTodoEndpoint(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/todos");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class WindowCounter {
        private volatile long windowStartEpochSeconds;
        private final AtomicInteger count = new AtomicInteger();

        private WindowCounter(long windowStartEpochSeconds) {
            this.windowStartEpochSeconds = windowStartEpochSeconds;
        }
    }
}
