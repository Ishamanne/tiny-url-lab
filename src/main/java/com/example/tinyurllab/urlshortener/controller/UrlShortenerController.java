package com.example.tinyurllab.urlshortener.controller;

import com.example.tinyurllab.urlshortener.dto.AnalyticsResponse;
import com.example.tinyurllab.urlshortener.dto.CreateShortUrlRequest;
import com.example.tinyurllab.urlshortener.dto.UpdateShortUrlRequest;
import com.example.tinyurllab.urlshortener.dto.ShortUrlResponse;
import com.example.tinyurllab.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "URL Shortener", description = "URL shortening and management API")
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @Operation(
            summary = "Create a short URL",
            description = "Creates a shortened URL from an original URL. Requires ADMIN role. This operation invalidates the URL caches and tracks latency metrics.",
            security = @SecurityRequirement(name = "basicAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(
                    examples = @ExampleObject(
                        value = "{\"originalUrl\":\"https://example.com\"}"
                    )
                )
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Short URL created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - check validation errors"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/shorten")
    @CacheEvict(value = {"shortUrls", "shortUrl"}, allEntries = true)
    @Timed(value = "url.shorten", description = "Time taken to create short URL")
    public ResponseEntity<ShortUrlResponse> createShortUrl(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrlResponse response = urlShortenerService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get all short URLs",
            description = "Retrieves a list of all shortened URLs with caching enabled for improved performance. Requires USER or ADMIN role.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all short URLs"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/api/urls")
    @Cacheable(value = "shortUrls")
    @Timed(value = "url.getAll", description = "Time taken to retrieve all URLs")
    public ResponseEntity<List<ShortUrlResponse>> getAllShortUrls() {
        List<ShortUrlResponse> urls = urlShortenerService.getAllShortUrls();
        return ResponseEntity.ok(urls);
    }

    @Operation(
            summary = "Get a specific short URL",
            description = "Retrieves details of a shortened URL by its short code with caching enabled. Requires USER or ADMIN role.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Short URL details"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Short URL not found")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/api/urls/{shortCode}")
    @Cacheable(value = "shortUrl", key = "#shortCode")
    @Timed(value = "url.getOne", description = "Time taken to retrieve single URL")
    public ResponseEntity<ShortUrlResponse> getShortUrl(
            @Parameter(description = "Short code of the URL") @PathVariable String shortCode) {
        ShortUrlResponse response = urlShortenerService.getShortUrl(shortCode);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get analytics for a short URL",
            description = "Retrieves click analytics for a shortened URL including total clicks and click history with caching enabled. Requires USER or ADMIN role.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Short URL not found")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/api/urls/{shortCode}/analytics")
    @Cacheable(value = "analytics", key = "#shortCode")
    @Timed(value = "url.analytics", description = "Time taken to retrieve analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @Parameter(description = "Short code of the URL") @PathVariable String shortCode) {
        AnalyticsResponse analytics = urlShortenerService.getAnalytics(shortCode);
        return ResponseEntity.ok(analytics);
    }

    @Operation(
            summary = "Update a short URL",
            description = "Updates a shortened URL's expiration time. Requires ADMIN role.",
            security = @SecurityRequirement(name = "basicAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(
                    examples = @ExampleObject(
                        value = "{\"expiresInMinutes\":60}"
                    )
                )
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Short URL updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Short URL not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/urls/{shortCode}")
    @CacheEvict(value = {"shortUrls", "shortUrl", "analytics"}, allEntries = true)
    @Timed(value = "url.update", description = "Time taken to update URL")
    public ResponseEntity<ShortUrlResponse> updateShortUrl(
            @Parameter(description = "Short code of the URL") @PathVariable String shortCode,
            @Valid @RequestBody UpdateShortUrlRequest request) {
        ShortUrlResponse response = urlShortenerService.updateShortUrl(shortCode, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete a short URL",
            description = "Marks a shortened URL as expired/deleted. Requires ADMIN role.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Short URL deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Short URL not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/urls/{shortCode}")
    @Caching(evict = {
            @CacheEvict(value = "shortUrls", allEntries = true),
            @CacheEvict(value = {"shortUrl", "analytics"}, key = "#shortCode")
    })
    @Timed(value = "url.delete", description = "Time taken to delete URL")
    public ResponseEntity<Void> deleteShortUrl(
            @Parameter(description = "Short code of the URL") @PathVariable String shortCode) {
        urlShortenerService.deleteShortUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Redirect to original URL", description = "Redirects to the original URL using the short code", hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
            @ApiResponse(responseCode = "404", description = "Short URL not found"),
            @ApiResponse(responseCode = "410", description = "URL expired")
    })
    @GetMapping("/{shortCode}")
    @Caching(evict = {
            @CacheEvict(value = "shortUrls", allEntries = true),
            @CacheEvict(value = "shortUrl", key = "#shortCode")
    })
    @Timed(value = "url.redirect", description = "Time taken to redirect to original URL")
    public void redirect(
            @Parameter(description = "Short code of the URL") @PathVariable String shortCode,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        log.info("Redirecting short code: {}", shortCode);
        String originalUrl = urlShortenerService.resolveShortCode(shortCode, request);
        response.sendRedirect(originalUrl);
    }
}
