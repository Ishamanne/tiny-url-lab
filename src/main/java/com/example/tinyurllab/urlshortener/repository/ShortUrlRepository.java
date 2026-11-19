package com.example.tinyurllab.urlshortener.repository;

import com.example.tinyurllab.urlshortener.model.ShortUrl;
import com.example.tinyurllab.urlshortener.model.UrlStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM ShortUrl s WHERE s.shortCode = :shortCode AND s.status = 'ACTIVE'")
    boolean existsByShortCodeAndActive(String shortCode);

    List<ShortUrl> findByCreatedBy(String createdBy);

    @Query("SELECT s FROM ShortUrl s WHERE s.status = :status AND (s.expiresAt IS NULL OR s.expiresAt > :now)")
    List<ShortUrl> findAllByStatus(UrlStatus status, Instant now);

    @Modifying
    @Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + 1 WHERE s.shortCode = :shortCode")
    void incrementClickCount(String shortCode);

    @Query("SELECT COUNT(s) FROM ShortUrl s WHERE s.createdAt >= :since")
    long countCreatedSince(Instant since);

    @Query("SELECT SUM(s.clickCount) FROM ShortUrl s WHERE s.shortCode = :shortCode")
    Long getTotalClicksByShortCode(String shortCode);
}
