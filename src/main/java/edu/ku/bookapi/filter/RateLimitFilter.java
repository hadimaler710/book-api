package edu.ku.bookapi.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ku.bookapi.config.BookProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple fixed-window, per-client rate limiter for the {@code /api/**} routes.
 *
 * <p>Each client (identified by {@code X-Forwarded-For} when present, otherwise
 * by the remote address) gets {@code limit} requests per {@code windowSeconds}.
 * Once the limit is exceeded the filter answers with HTTP 429 and a JSON body
 * shaped like the standard {@code ErrorResponse}.</p>
 *
 * <p>This is a deliberately dependency-free implementation (no Bucket4j) that
 * is disabled by default and can be enabled per environment via
 * {@code book-api.rate-limit.enabled}. For clustered deployments a shared
 * store (Redis) would be required instead.</p>
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    /** Bucket-eviction trigger to bound memory usage. */
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BookProperties.RateLimit settings;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @param settings rate-limit configuration (enabled, limit, windowSeconds)
     */
    public RateLimitFilter(BookProperties.RateLimit settings) {
        this.settings = settings;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !settings.isEnabled() || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientKey(request);
        Window window = windows.computeIfAbsent(key, k -> new Window());

        long now = System.currentTimeMillis();
        long windowMillis = settings.getWindowSeconds() * 1000L;
        long currentStart = window.start.get();
        if (now - currentStart > windowMillis && window.start.compareAndSet(currentStart, now)) {
            window.count.set(0);
            evictStaleWindows(now, windowMillis);
        }

        if (window.count.incrementAndGet() > settings.getLimit()) {
            log.warn("Rate limit exceeded for client [{}] on {}", key, request.getRequestURI());
            respondTooManyRequests(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void evictStaleWindows(long now, long windowMillis) {
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().start.get() > windowMillis);
        }
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void respondTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", "Rate limit exceeded. Please slow down and try again later.");
        body.put("path", request.getRequestURI());

        response.setStatus(429);
        response.setContentType("application/json");
        MAPPER.writeValue(response.getWriter(), body);
    }

    /** Fixed window: start timestamp plus request counter. */
    private static final class Window {
        private final AtomicLong start = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger count = new AtomicInteger();
    }
}