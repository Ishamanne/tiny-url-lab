package com.example.tinyurllab.urlshortener.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "short_urls", indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    @NotBlank
    private String shortCode;

    @Column(nullable = false, length = 2048)
    @NotBlank
    private String originalUrl;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    @Column
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UrlStatus status = UrlStatus.ACTIVE;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public UrlStatus getComputedStatus() {
        return status;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public void incrementClickCount() {
        this.clickCount++;
    }
}
