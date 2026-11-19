package com.example.tinyurllab.urlshortener.controller;

import com.example.tinyurllab.urlshortener.dto.CreateShortUrlRequest;
import com.example.tinyurllab.urlshortener.dto.UpdateShortUrlRequest;
import com.example.tinyurllab.urlshortener.repository.ShortUrlRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UrlShortenerController
 * Tests full request/response flow with security, validation, and error handling
 */
@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @BeforeEach
    void setUp() {
        shortUrlRepository.deleteAll();
    }

    @Test
    void createShortUrl_WithoutAuth_Returns401() throws Exception {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createShortUrl_WithUserRole_Returns403() throws Exception {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createShortUrl_WithAdminRole_Returns201() throws Exception {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
                .andExpect(jsonPath("$.shortUrl").value(containsString("/r/")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.clickCount").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createShortUrl_WithInvalidUrl_Returns400() throws Exception {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("invalid-url");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createShortUrl_WithCustomAlias_ReturnsCustomShortCode() throws Exception {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomAlias("custom123");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("custom123"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllShortUrls_WithUserRole_Returns200() throws Exception {
        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getShortUrl_NonExistent_Returns404() throws Exception {
        mockMvc.perform(get("/api/urls/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateShortUrl_ExtendsExpiration_Returns200() throws Exception {
        // Create URL first
        CreateShortUrlRequest createRequest = new CreateShortUrlRequest();
        createRequest.setOriginalUrl("https://example.com");

        String createResponse = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String shortCode = objectMapper.readTree(createResponse).get("shortCode").asText();

        // Update expiration
        UpdateShortUrlRequest updateRequest = new UpdateShortUrlRequest();
        updateRequest.setExpiresInMinutes(120L);

        mockMvc.perform(patch("/api/urls/{shortCode}", shortCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAnalytics_ReturnsAnalyticsData() throws Exception {
        // Create URL first
        CreateShortUrlRequest createRequest = new CreateShortUrlRequest();
        createRequest.setOriginalUrl("https://example.com");

        String createResponse = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(request -> {
                            request.addUserRole("ADMIN");
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String shortCode = objectMapper.readTree(createResponse).get("shortCode").asText();

        // Get analytics
        mockMvc.perform(get("/api/urls/{shortCode}/analytics", shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.totalClicks").isNumber())
                .andExpect(jsonPath("$.deviceDistribution").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteShortUrl_ValidShortCode_Returns204() throws Exception {
        // Create URL first
        CreateShortUrlRequest createRequest = new CreateShortUrlRequest();
        createRequest.setOriginalUrl("https://example.com");

        String createResponse = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String shortCode = objectMapper.readTree(createResponse).get("shortCode").asText();

        // Delete URL
        mockMvc.perform(delete("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shortCode\":\"" + shortCode + "\"}"))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/urls/{shortCode}", shortCode))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createShortUrl_CacheInvalidation_WorksCorrectly() throws Exception {
        // Create first URL
        CreateShortUrlRequest request1 = new CreateShortUrlRequest();
        request1.setOriginalUrl("https://example.com/1");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Get all URLs (caches result)
        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Create second URL (should invalidate cache)
        CreateShortUrlRequest request2 = new CreateShortUrlRequest();
        request2.setOriginalUrl("https://example.com/2");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Get all URLs again (should show updated list)
        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
