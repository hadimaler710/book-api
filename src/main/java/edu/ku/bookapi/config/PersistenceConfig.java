package edu.ku.bookapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA-specific configuration.
 *
 * <p>Kept separate from {@link ApplicationConfig} (which is a
 * {@code WebMvcConfigurer}) so that web-only test slices load the MVC beans
 * without requiring a JPA metamodel.</p>
 */
@Configuration
@EnableJpaAuditing
public class PersistenceConfig {
}