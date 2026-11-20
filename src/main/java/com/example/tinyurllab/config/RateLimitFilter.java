package com.example.tinyurllab.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private record Window(long startMillis, AtomicInteger count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final long windowMillis;
    private final Counter allowedCounter;
    private final Counter rejectedCounter;
    private final String trustedProxies;

    public RateLimitFilter(
            MeterRegistry meterRegistry,
            @Value("${app.rate-limit.requests-per-minute:100}") int limit,
            @Value("${app.rate-limit.window-ms:60000}") long windowMillis,
            @Value("${app.rate-limit.trusted-proxies:}") String trustedProxies) {
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.trustedProxies = trustedProxies;
        this.allowedCounter = meterRegistry.counter("url.shorten.rate_limit.allowed");
        this.rejectedCounter = meterRegistry.counter("url.shorten.rate_limit.rejected");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !"/api/shorten".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String clientKey = resolveClientKey(request);
        long now = Instant.now().toEpochMilli();
        Window window = windows.compute(clientKey, (key, existing) -> {
            if (existing == null || now - existing.startMillis() >= windowMillis) {
                return new Window(now, new AtomicInteger());
            }
            return existing;
        });

        int count = window.count().incrementAndGet();
        if (count > limit) {
            rejectedCounter.increment();
            long retryAfterSeconds = Math.max(1,
                    (window.startMillis() + windowMillis - now) / 1000);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many short URL requests\"}");
            return;
        }

        allowedCounter.increment();
        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        boolean trusted = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .anyMatch(remote::equals);
        if (!trusted) {
            return remote;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remote;
        }
        return forwarded.split(",")[0].trim();
    }
}
