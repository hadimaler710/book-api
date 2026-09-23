package edu.ku.bookapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import edu.ku.bookapi.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Core application configuration: type-safe properties, CORS, caching and the
 * optional rate-limit filter.
 *
 * <p>JPA auditing intentionally lives in {@link PersistenceConfig} so that
 * web-only test slices ({@code @WebMvcTest}) never need a JPA metamodel.</p>
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(BookProperties.class)
@RequiredArgsConstructor
public class ApplicationConfig implements WebMvcConfigurer {

    private final BookProperties properties;

    /**
     * CORS rules for browser-based development clients.
     * Allowed origins are externalized to {@code book-api.cors.allowed-origins}.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.getCors().getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    /**
     * Caffeine-backed cache manager with two named caches:
     * <ul>
     *     <li>{@code books} - cached result pages of GET /books (keyed by Pageable)</li>
     *     <li>{@code book} - cached single books (keyed by id)</li>
     * </ul>
     * TTL and maximum size are configurable via {@code book-api.cache.*}.
     */
    @Bean
    public CacheManager cacheManager(BookProperties bookProperties) {
        BookProperties.Cache cache = bookProperties.getCache();
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("books", "book");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(cache.getTimeToLive())
                .maximumSize(cache.getMaximumSize())
                .recordStats());
        return cacheManager;
    }

    /**
     * Registers the rate-limit filter for {@code /api/*} only when
     * {@code book-api.rate-limit.enabled=true} (disabled by default).
     */
    @Bean
    @ConditionalOnProperty(prefix = "book-api.rate-limit", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(BookProperties bookProperties) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(bookProperties.getRateLimit()));
        registration.addUrlPatterns("/api/*");
        registration.setName("rateLimitFilter");
        registration.setOrder(1);
        return registration;
    }
}