package com.example.tinyurllab.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateOpenApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.openapi").value("3.0.1"))
                .andExpect(jsonPath("$.info.title").exists())
                .andExpect(jsonPath("$.paths").exists());
    }

    @Test
    void shouldAccessSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void shouldHaveUrlShortenerEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/shorten']").exists())
                .andExpect(jsonPath("$.paths['/api/urls']").exists())
                .andExpect(jsonPath("$.paths['/api/urls/{shortCode}']").exists())
                .andExpect(jsonPath("$.paths['/api/urls/{shortCode}/analytics']").exists())
                .andExpect(jsonPath("$.paths['/{shortCode}']").exists());
    }

    @Test
    void shouldHaveSchemaDefinitions() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.CreateShortUrlRequest").exists())
                .andExpect(jsonPath("$.components.schemas.ShortUrlResponse").exists())
                .andExpect(jsonPath("$.components.schemas.AnalyticsResponse").exists());
    }
}
