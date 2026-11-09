package com.mello.bookingsystem;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends AbstractIntegrationTest {

    @Test
    void registerReturnsToken() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"email\":\"a@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void emailIsCaseInsensitiveSoDuplicatesConflict() throws Exception {
        register("a@test.com");
        mvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"email\":\"A@Test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"email\":\"a@test.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        register("a@test.com");
        mvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"a@test.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointNeedsToken() throws Exception {
        mvc.perform(get("/api/tables")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = register("a@test.com");
        mvc.perform(get("/api/tables").with(as(token + "x"))).andExpect(status().isUnauthorized());
    }

    @Test
    void normalUserCannotCreateTables() throws Exception {
        String token = register("a@test.com");
        mvc.perform(post("/api/tables").with(as(token)).contentType(JSON)
                        .content("{\"label\":\"T9\",\"seats\":4,\"zone\":\"INDOOR\"}"))
                .andExpect(status().isForbidden());
    }
}