package com.example.tinyurllab.urlshortener.repository;

import com.example.tinyurllab.urlshortener.model.UrlAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UrlAnalyticsRepository extends JpaRepository<UrlAnalytics, Long> {

    List<UrlAnalytics> findByShortCode(String shortCode);

    @Query("SELECT COUNT(a) FROM UrlAnalytics a WHERE a.shortCode = :shortCode")
    long countByShortCode(String shortCode);

    @Query("SELECT COUNT(a) FROM UrlAnalytics a WHERE a.shortCode = :shortCode AND a.accessedAt >= :since")
    long countByShortCodeSince(String shortCode, Instant since);

    @Query("SELECT a.country, COUNT(a) FROM UrlAnalytics a WHERE a.shortCode = :shortCode GROUP BY a.country")
    List<Object[]> getCountryStatistics(String shortCode);

    @Query("SELECT a.device, COUNT(a) FROM UrlAnalytics a WHERE a.shortCode = :shortCode GROUP BY a.device")
    List<Object[]> getDeviceStatistics(String shortCode);

    @Query("SELECT a FROM UrlAnalytics a WHERE a.shortCode = :shortCode AND a.accessedAt >= :since AND a.accessedAt <= :until ORDER BY a.accessedAt DESC")
    List<UrlAnalytics> findByShortCodeBetween(String shortCode, Instant since, Instant until);
}
