package com.example.tinyurllab.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateShortUrlRequest {

    @NotBlank(message = "Original URL is required")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String originalUrl;

    private String customAlias;

    @Positive(message = "Expiration time must be positive")
    private Long expiresInMinutes;

    private String createdBy;
}
