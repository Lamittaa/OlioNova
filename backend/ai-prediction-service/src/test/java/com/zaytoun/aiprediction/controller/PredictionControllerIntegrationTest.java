package com.zaytoun.aiprediction.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PredictionControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.cloud.config.enabled", () -> false);
        registry.add("eureka.client.enabled", () -> false);
        registry.add("security.api-key.internal", () -> "internal-test-key");
        registry.add("security.api-key.admin", () -> "admin-test-key");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void predictShouldRejectUnsafeBatchId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "olive.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/predict")
                        .file(file)
                        .param("batchId", "../bad-batch")
                        .header("X-API-Key", "internal-test-key"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "batchId must be 1-64 characters and contain only letters, numbers, hyphens, or underscores"
                ));
    }
}
