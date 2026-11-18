package com.example.tinyurllab.urlshortener.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AnalyticsResponse {
    private String shortCode;
    private Long totalClicks;
    private Long clicksLast24Hours;
    private Long clicksLast7Days;
    private Map<String, Long> countryDistribution;
    private Map<String, Long> deviceDistribution;
}
