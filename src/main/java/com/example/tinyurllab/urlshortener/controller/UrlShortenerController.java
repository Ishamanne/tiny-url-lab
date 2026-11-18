package com.example.tinyurllab.urlshortener.controller;

import com.example.tinyurllab.urlshortener.dto.AnalyticsResponse;
import com.example.tinyurllab.urlshortener.dto.CreateShortUrlRequest;
import com.example.tinyurllab.urlshortener.dto.ShortUrlResponse;
import com.example.tinyurllab.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "URL Shortener", description = "URL shortening and management API")
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @Operation(summary = "Create a short URL", description = "Creates a shortened URL from an original URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Short URL created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/api/shorten")
    public ResponseEntity<ShortUrlResponse> createShortUrl(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrlResponse response = urlShortenerService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all short URLs", description = "Retrieves a list of all shortened URLs")
    @ApiResponse(responseCode = "200", description = "List of all short URLs")
    @GetMapping("/api/urls")
    public ResponseEntity<List<ShortUrlResponse>> getAllShortUrls() {
        List<ShortUrlResponse> urls = urlShortenerService.getAllShortUrls();
        return ResponseEntity.ok(urls);
    }

    @Operation(summary = "Get a specific short URL", description = "Retrieves details of a shortened URL by its short code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Short URL details"),
            @ApiResponse(responseCode = "404", description = "Short URL not found")
    })
    @GetMapping("/api/urls/{shortCode}")
    public ResponseEntity<ShortUrlResponse> getShortUrl(
            @Parameter(description = "Short code of the URL") @PathVariable String shortCode) {
        ShortUrlResponse response = urlShortenerService.getShortUrl(shortCode);
        return ResponseEntity.ok(response);
    }

    /*
     * This method retrieves analytics for a short URL.
     */
    @Operation(summary = "Get analytics for a short URL", description = "Retrieves click analytics for a shortened URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics data"),
            @ApiResponse(responseCode = "404", description = "Short URL not found")
    })
    @GetMapping("/api/urls/{shortCode}/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @Parameter(description = "Short code of the URL") @PathVariable String shortCode) {
        AnalyticsResponse analytics = urlShortenerService.getAnalytics(shortCode);
        return ResponseEntity.ok(analytics);
    }

    @Operation(summary = "Delete a short URL", description = "Deletes a shortened URL by its short code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Short URL deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Short URL not found")
    })
    @DeleteMapping("/api/urls/{shortCode}")
    public ResponseEntity<Void> deleteShortUrl(
            @Parameter(description = "Short code of the URL") @PathVariable String shortCode) {
        urlShortenerService.deleteShortUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Redirect to original URL", description = "Redirects to the original URL using the short code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
            @ApiResponse(responseCode = "404", description = "Short URL not found"),
            @ApiResponse(responseCode = "410", description = "URL expired")
    })
    @GetMapping("/{shortCode}")
    public void redirect(
            @Parameter(description = "Short code of the URL") @PathVariable String shortCode,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        log.info("Redirecting short code: {}", shortCode);
        String originalUrl = urlShortenerService.resolveShortCode(shortCode, request);
        response.sendRedirect(originalUrl);
    }
}
