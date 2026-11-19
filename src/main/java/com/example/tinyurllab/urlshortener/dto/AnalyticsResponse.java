package com.example.tinyurllab.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@Schema(description = "Analytics data for a shortened URL (Note: Data is volatile - stored in H2 in-memory database)")
public class AnalyticsResponse {
    @Schema(description = "Short code of the URL")
    private String shortCode;

    @Schema(description = "Total number of clicks since URL creation (resets on app restart)")
    private Long totalClicks;

    @Schema(description = "Distribution of clicks by device type (mobile/desktop)")
    private Map<String, Long> deviceDistribution;
}
