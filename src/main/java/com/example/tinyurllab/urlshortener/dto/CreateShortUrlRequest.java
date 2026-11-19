package com.example.tinyurllab.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Request to create a shortened URL")
public class CreateShortUrlRequest {

    @NotBlank(message = "Original URL is required")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    @Schema(description = "The original URL to shorten", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    private String originalUrl;

    @Schema(description = "Optional custom alias for the short URL (leave empty for auto-generated)", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private String customAlias;

    @Positive(message = "Expiration time must be positive")
    @Schema(description = "Optional expiration time in minutes (default: 7 minutes if not specified)", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private Long expiresInMinutes;
}
