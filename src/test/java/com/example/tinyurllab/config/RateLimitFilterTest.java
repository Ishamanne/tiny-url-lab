package com.example.tinyurllab.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(new SimpleMeterRegistry(), 2, 60_000, "");
    }

    @Test
    void postShortenBeyondLimitReturns429() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/shorten");
        request.setRemoteAddr("127.0.0.1");
        FilterChain chain = (servletRequest, servletResponse) -> {};

        filter.doFilter(request, new MockHttpServletResponse(), chain);
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        MockHttpServletResponse rejected = new MockHttpServletResponse();
        filter.doFilter(request, rejected, chain);

        assertEquals(429, rejected.getStatus());
        assertEquals(true, Integer.parseInt(rejected.getHeader("Retry-After")) > 0);
    }

    @Test
    void getRequestIsNotFiltered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/urls");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

        assertEquals(200, response.getStatus());
    }
}
