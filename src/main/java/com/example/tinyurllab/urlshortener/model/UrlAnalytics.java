package com.example.tinyurllab.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "url_analytics", indexes = {
        @Index(name = "idx_short_code_analytics", columnList = "shortCode"),
        @Index(name = "idx_accessed_at", columnList = "accessedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String shortCode;

    @Column(nullable = false)
    private Instant accessedAt;

    @Column(length = 100)
    private String referer;

    @Column(length = 200)
    private String userAgent;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 50)
    private String country;

    @Column(length = 50)
    private String device;

    @PrePersist
    protected void onCreate() {
        accessedAt = Instant.now();
    }
}
