package com.mello.bookingsystem;

import com.jayway.jsonpath.JsonPath;
import com.mello.bookingsystem.booking.BookingRepository;
import com.mello.bookingsystem.diningtable.DiningTableRepository;
import com.mello.bookingsystem.user.Role;
import com.mello.bookingsystem.user.User;
import com.mello.bookingsystem.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class AbstractIntegrationTest {

    protected static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired protected MockMvc mvc;
    @Autowired protected UserRepository users;
    @Autowired protected DiningTableRepository tables;
    @Autowired protected BookingRepository bookings;

    @BeforeEach
    void cleanDatabase() {
        bookings.deleteAll();   // children first, because of the foreign keys
        tables.deleteAll();
        users.deleteAll();
    }

    /** Adds "Authorization: Bearer <token>" to a request. */
    protected RequestPostProcessor as(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    protected String register(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    /** Registers an admin directly in the database, then logs in to get a token containing the ADMIN role. */
    protected String adminToken() throws Exception {
        register("admin@test.com");
        User admin = users.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        users.save(admin);
        String body = mvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    protected long createTable(String adminToken, String label, int seats) throws Exception {
        String body = mvc.perform(post("/api/tables").with(as(adminToken)).contentType(JSON)
                        .content("{\"label\":\"%s\",\"seats\":%d,\"zone\":\"INDOOR\"}".formatted(label, seats)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected ResultActions book(String token, long tableId, Instant start, Instant end, int party) throws Exception {
        return mvc.perform(post("/api/bookings").with(as(token)).contentType(JSON)
                .content("{\"tableId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\",\"partySize\":%d}"
                        .formatted(tableId, start, end, party)));
    }

    protected static long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** A time 30 days from now at the given hour of the day (UTC), so bookings are always in the future. */
    protected static Instant slot(int hour) {
        return Instant.now().plus(Duration.ofDays(30)).truncatedTo(ChronoUnit.DAYS).plus(Duration.ofHours(hour));
    }
}