package com.example.tinyurllab.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to delete a shortened URL")
public class DeleteShortUrlRequest {

    @NotBlank(message = "Short code is required")
    @Schema(description = "The short code to delete", requiredMode = Schema.RequiredMode.REQUIRED)
    private String shortCode;
}
