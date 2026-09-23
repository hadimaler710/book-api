package edu.ku.bookapi.filter;

import edu.ku.bookapi.config.BookProperties;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the fixed-window {@link RateLimitFilter}.
 */
class RateLimitFilterTest {

    private BookProperties.RateLimit settings;

    @BeforeEach
    void setUp() {
        settings = new BookProperties.RateLimit();
        settings.setEnabled(true);
        settings.setLimit(3);
        settings.setWindowSeconds(60);
    }

    @Test
    void blocksRequestsOnceLimitIsExceeded() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(settings);

        for (int i = 0; i < 3; i++) {
            assertThat(status(filter, "1.2.3.4", null)).isEqualTo(200);
        }
        assertThat(status(filter, "1.2.3.4", null)).isEqualTo(429);
        assertThat(status(filter, "1.2.3.4", null)).isEqualTo(429);
    }

    @Test
    void countsClientsIndependently() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(settings);

        for (int i = 0; i < 3; i++) {
            assertThat(status(filter, "1.2.3.4", null)).isEqualTo(200);
        }
        assertThat(status(filter, "1.2.3.4", null)).isEqualTo(429);
        // A different client still has its full budget
        assertThat(status(filter, "5.6.7.8", null)).isEqualTo(200);
    }

    @Test
    void usesForwardedForHeaderWhenPresent() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(settings);

        for (int i = 0; i < 3; i++) {
            assertThat(status(filter, "10.0.0.1", "9.9.9.9")).isEqualTo(200);
        }
        // The forwarded client is exhausted...
        assertThat(status(filter, "10.0.0.1", "9.9.9.9")).isEqualTo(429);
        // ...but the actual remote address bucket is untouched
        assertThat(status(filter, "10.0.0.1", null)).isEqualTo(200);
    }

    @Test
    void disabledFilterNeverBlocks() throws ServletException, IOException {
        settings.setEnabled(false);
        RateLimitFilter filter = new RateLimitFilter(settings);

        for (int i = 0; i < 25; i++) {
            assertThat(status(filter, "1.2.3.4", null)).isEqualTo(200);
        }
    }

    @Test
    void skipsNonApiPaths() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(settings);

        for (int i = 0; i < 25; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
            request.setRemoteAddr("1.2.3.4");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private int status(RateLimitFilter filter, String remoteAddr, String forwardedFor)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books");
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }
}