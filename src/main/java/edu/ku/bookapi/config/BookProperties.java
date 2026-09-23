package edu.ku.bookapi.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Type-safe configuration bound from the {@code book-api.*} namespace in the
 * YAML files. Every property has a sane default so the application starts
 * even without an explicit configuration entry.
 *
 * @see ApplicationConfig
 */
@Data
@Validated
@ConfigurationProperties(prefix = "book-api")
public class BookProperties {

    /** Sample-data seeding behaviour. */
    private Seed seed = new Seed();

    /** CORS settings for browser clients. */
    private Cors cors = new Cors();

    /** Optional API rate limiting. */
    private RateLimit rateLimit = new RateLimit();

    /** Caffeine cache tuning. */
    private Cache cache = new Cache();

    /** {@code book-api.seed.*} */
    @Data
    public static class Seed {
        /** Insert demo books when the database is empty. */
        private boolean enabled = true;
    }

    /** {@code book-api.cors.*} */
    @Data
    public static class Cors {
        /** Origins allowed to call the API from a browser. */
        private List<String> allowedOrigins = List.of(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:5173",
                "http://localhost:8081");
    }

    /** {@code book-api.rate-limit.*} */
    @Data
    public static class RateLimit {
        /** Enable the per-client rate limit filter. */
        private boolean enabled = false;
        /** Maximum requests per client inside the window. */
        @Min(1)
        private int limit = 100;
        /** Window length in seconds. */
        @Min(1)
        private int windowSeconds = 60;
    }

    /** {@code book-api.cache.*} */
    @Data
    public static class Cache {
        /** Time cached entries stay in the cache. */
        private Duration timeToLive = Duration.ofMinutes(5);
        /** Maximum number of entries per cache. */
        @Min(1)
        private long maximumSize = 1000;
    }
}