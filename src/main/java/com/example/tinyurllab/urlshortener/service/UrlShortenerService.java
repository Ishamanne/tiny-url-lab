package com.example.tinyurllab.urlshortener.service;

import com.example.tinyurllab.urlshortener.dto.AnalyticsResponse;
import com.example.tinyurllab.urlshortener.dto.CreateShortUrlRequest;
import com.example.tinyurllab.urlshortener.dto.ShortUrlResponse;
import com.example.tinyurllab.urlshortener.exception.UrlNotFoundException;
import com.example.tinyurllab.urlshortener.exception.UrlExpiredException;
import com.example.tinyurllab.urlshortener.model.ShortUrl;
import com.example.tinyurllab.urlshortener.model.UrlAnalytics;
import com.example.tinyurllab.urlshortener.model.UrlStatus;
import com.example.tinyurllab.urlshortener.repository.ShortUrlRepository;
import com.example.tinyurllab.urlshortener.repository.UrlAnalyticsRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final ShortUrlRepository shortUrlRepository;
    private final UrlAnalyticsRepository analyticsRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public ShortUrlResponse createShortUrl(CreateShortUrlRequest request) {
        log.info("Creating short URL for: {}", request.getOriginalUrl());

        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            if (shortUrlRepository.existsByShortCode(request.getCustomAlias())) {
                throw new IllegalArgumentException("Custom alias already exists");
            }
            shortCode = request.getCustomAlias();
        } else {
            shortCode = generateUniqueShortCode();
        }

        // Set expiration based on expiresInMinutes or default to 8 days
        Instant expiresAt;
        if (request.getExpiresInMinutes() != null && request.getExpiresInMinutes() > 0) {
            expiresAt = Instant.now().plus(request.getExpiresInMinutes(), ChronoUnit.MINUTES);
        } else {
            expiresAt = Instant.now().plus(8, ChronoUnit.DAYS);
        }

        ShortUrl shortUrl = ShortUrl.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .expiresAt(expiresAt)
                .createdBy(request.getCreatedBy())
                .clickCount(0L)
                .build();

        shortUrl = shortUrlRepository.save(shortUrl);
        log.info("Short URL created: {} -> {}", shortCode, request.getOriginalUrl());

        return toResponse(shortUrl);
    }

    @Transactional
    public String resolveShortCode(String shortCode, HttpServletRequest request) {
        log.debug("Resolving short code: {}", shortCode);

        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        UrlStatus computedStatus = shortUrl.getComputedStatus();

        if (computedStatus == UrlStatus.DELETED) {
            throw new UrlNotFoundException("Short URL has been deleted: " + shortCode);
        }

        if (computedStatus == UrlStatus.EXPIRED) {
            throw new UrlExpiredException("Short URL has expired: " + shortCode);
        }

        // Track analytics
        trackAccess(shortUrl, request);

        // Increment click count
        shortUrl.incrementClickCount();
        shortUrlRepository.save(shortUrl);

        return shortUrl.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse getShortUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));
        return toResponse(shortUrl);
    }

    @Transactional(readOnly = true)
    public List<ShortUrlResponse> getAllShortUrls() {
        return shortUrlRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String shortCode) {
        if (!shortUrlRepository.existsByShortCode(shortCode)) {
            throw new UrlNotFoundException("Short URL not found: " + shortCode);
        }

        Long totalClicks = analyticsRepository.countByShortCode(shortCode);
        Long clicks24h = analyticsRepository.countByShortCodeSince(shortCode, Instant.now().minus(24, ChronoUnit.HOURS));
        Long clicks7d = analyticsRepository.countByShortCodeSince(shortCode, Instant.now().minus(7, ChronoUnit.DAYS));

        Map<String, Long> countryDistribution = analyticsRepository.getCountryStatistics(shortCode).stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));

        Map<String, Long> deviceDistribution = analyticsRepository.getDeviceStatistics(shortCode).stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));

        return AnalyticsResponse.builder()
                .shortCode(shortCode)
                .totalClicks(totalClicks)
                .clicksLast24Hours(clicks24h)
                .clicksLast7Days(clicks7d)
                .countryDistribution(countryDistribution)
                .deviceDistribution(deviceDistribution)
                .build();
    }
/*
 * This method deletes a short URL from the system.
 */
    @Transactional
    public void deleteShortUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));
        shortUrl.setStatus(UrlStatus.DELETED);
        shortUrlRepository.save(shortUrl);
        log.info("Short URL deleted: {}", shortCode);
    }

    private void trackAccess(ShortUrl shortUrl, HttpServletRequest request) {
        UrlAnalytics analytics = UrlAnalytics.builder()
                .shortCode(shortUrl.getShortCode())
                .referer(request.getHeader("Referer"))
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(getClientIp(request))
                .country(extractCountryFromIp(getClientIp(request)))
                .device(extractDeviceFromUserAgent(request.getHeader("User-Agent")))
                .build();

        analyticsRepository.save(analytics);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String extractCountryFromIp(String ip) {
        return null;
    }

    private String extractDeviceFromUserAgent(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobile")) return "Mobile";
        if (userAgent.contains("Tablet")) return "Tablet";
        return "Desktop";
    }

    private String generateUniqueShortCode() {
        String shortCode;
        do {
            shortCode = generateRandomCode();
        } while (shortUrlRepository.existsByShortCode(shortCode));
        return shortCode;
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private ShortUrlResponse toResponse(ShortUrl shortUrl) {
        return ShortUrlResponse.builder()
                .shortCode(shortUrl.getShortCode())
                .shortUrl(baseUrl + "/" + shortUrl.getShortCode())
                .originalUrl(shortUrl.getOriginalUrl())
                .createdAt(shortUrl.getCreatedAt())
                .expiresAt(shortUrl.getExpiresAt())
                .clickCount(shortUrl.getClickCount())
                .status(shortUrl.getComputedStatus())
                .build();
    }
}
