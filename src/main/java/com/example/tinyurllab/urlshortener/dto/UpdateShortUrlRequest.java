package com.example.tinyurllab.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Request to update a shortened URL")
public class UpdateShortUrlRequest {


    @Positive(message = "Expiration time must be positive")
    @Schema(description = "New expiration time in minutes from now", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private Long expiresInMinutes;
}
