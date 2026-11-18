package com.example.tinyurllab.urlshortener.dto;

import com.example.tinyurllab.urlshortener.model.UrlStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ShortUrlResponse {
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private Instant createdAt;
    private Instant expiresAt;
    private Long clickCount;
    private UrlStatus status;
}
