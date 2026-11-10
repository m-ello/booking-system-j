package com.mello.bookingsystem;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiTest extends AbstractIntegrationTest {

    @Test
    void apiDocsArePublicAndListTheEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/bookings']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
    }
}