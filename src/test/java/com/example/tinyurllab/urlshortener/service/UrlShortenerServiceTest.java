package com.example.tinyurllab.urlshortener.service;

import com.example.tinyurllab.urlshortener.dto.CreateShortUrlRequest;
import com.example.tinyurllab.urlshortener.dto.ShortUrlResponse;
import com.example.tinyurllab.urlshortener.exception.UrlExpiredException;
import com.example.tinyurllab.urlshortener.exception.UrlNotFoundException;
import com.example.tinyurllab.urlshortener.model.ShortUrl;
import com.example.tinyurllab.urlshortener.model.UrlStatus;
import com.example.tinyurllab.urlshortener.repository.ShortUrlRepository;
import com.example.tinyurllab.urlshortener.repository.UrlAnalyticsRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UrlShortenerServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private UrlAnalyticsRepository analyticsRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlShortenerService, "baseUrl", "http://localhost:8080");

        // Mock security context
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createShortUrl_Success() {
        // Arrange
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com");

        ShortUrl savedUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .createdAt(Instant.now())
                .clickCount(0L)
                .build();

        when(shortUrlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(savedUrl);

        // Act
        ShortUrlResponse response = urlShortenerService.createShortUrl(request);

        // Assert
        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());
        assertEquals("https://example.com", response.getOriginalUrl());
        verify(shortUrlRepository).save(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_WithCustomCode_Success() {
        // Arrange
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomAlias("custom");

        ShortUrl savedUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("custom")
                .originalUrl("https://example.com")
                .createdAt(Instant.now())
                .clickCount(0L)
                .build();

        when(shortUrlRepository.existsByShortCodeAndActive("custom")).thenReturn(false);
        when(shortUrlRepository.findByShortCode("custom")).thenReturn(Optional.empty());
        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(savedUrl);

        // Act
        ShortUrlResponse response = urlShortenerService.createShortUrl(request);

        // Assert
        assertEquals("custom", response.getShortCode());
    }

    @Test
    void createShortUrl_CustomCodeExists_ThrowsException() {
        // Arrange
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomAlias("existing");

        when(shortUrlRepository.existsByShortCodeAndActive("existing")).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> urlShortenerService.createShortUrl(request));
    }

    @Test
    void resolveShortCode_Success() {
        // Arrange
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .createdAt(Instant.now())
                .clickCount(0L)
                .build();

        when(shortUrlRepository.findByShortCode("abc123")).thenReturn(Optional.of(shortUrl));
        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(shortUrl);
        when(httpServletRequest.getHeader(anyString())).thenReturn("test");
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        String result = urlShortenerService.resolveShortCode("abc123", httpServletRequest);

        // Assert
        assertEquals("https://example.com", result);
        verify(analyticsRepository).save(any());
        verify(shortUrlRepository).save(any(ShortUrl.class));
    }

    @Test
    void resolveShortCode_NotFound_ThrowsException() {
        // Arrange
        when(shortUrlRepository.findByShortCode("notfound")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UrlNotFoundException.class,
                () -> urlShortenerService.resolveShortCode("notfound", httpServletRequest));
    }

    @Test
    void resolveShortCode_Expired_Status_ThrowsException() {
        // Arrange
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .status(UrlStatus.EXPIRED)
                .build();

        when(shortUrlRepository.findByShortCode("abc123")).thenReturn(Optional.of(shortUrl));

        // Act & Assert
        assertThrows(UrlExpiredException.class,
                () -> urlShortenerService.resolveShortCode("abc123", httpServletRequest));
    }

    @Test
    void resolveShortCode_Expired_ThrowsException() {
        // Arrange
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        when(shortUrlRepository.findByShortCode("abc123")).thenReturn(Optional.of(shortUrl));

        // Act & Assert
        assertThrows(UrlExpiredException.class,
                () -> urlShortenerService.resolveShortCode("abc123", httpServletRequest));
    }

    @Test
    void getShortUrl_Success() {
        // Arrange
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .createdAt(Instant.now())
                .clickCount(10L)
                .build();

        when(shortUrlRepository.findByShortCode("abc123")).thenReturn(Optional.of(shortUrl));

        // Act
        ShortUrlResponse response = urlShortenerService.getShortUrl("abc123");

        // Assert
        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());
        assertEquals(10L, response.getClickCount());
    }

    @Test
    void deleteShortUrl_Success() {
        // Arrange
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .build();

        when(shortUrlRepository.findByShortCode("abc123")).thenReturn(Optional.of(shortUrl));
        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(shortUrl);

        // Act
        urlShortenerService.deleteShortUrl("abc123");

        // Assert
        verify(shortUrlRepository).delete(any(ShortUrl.class));
    }
}
