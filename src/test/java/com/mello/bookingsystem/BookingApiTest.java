package com.mello.bookingsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingApiTest extends AbstractIntegrationTest {

    String admin;
    long table;

    @BeforeEach
    void data() throws Exception {      // runs after the base class cleans the database
        admin = adminToken();
        table = createTable(admin, "T1", 4);
    }

    @Test
    void overlapRulesAreEnforced() throws Exception {
        String user = register("u@test.com");
        book(user, table, slot(18), slot(20), 2).andExpect(status().isCreated());
        book(user, table, slot(18), slot(20), 2).andExpect(status().isConflict());  // identical
        book(user, table, slot(19), slot(21), 2).andExpect(status().isConflict());  // partial overlap
        book(user, table, slot(17), slot(21), 2).andExpect(status().isConflict());  // contains the existing one
        book(user, table, slot(20), slot(22), 2).andExpect(status().isCreated());   // starts when the other ends
        book(user, table, slot(16), slot(18), 2).andExpect(status().isCreated());   // ends when the other starts
    }

    @Test
    void partyLargerThanTableIsRejected() throws Exception {
        String user = register("u@test.com");
        book(user, table, slot(18), slot(20), 5).andExpect(status().isBadRequest());
    }

    @Test
    void bookingInThePastIsRejected() throws Exception {
        String user = register("u@test.com");
        Instant now = Instant.now();
        book(user, table, now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(1)), 2)
                .andExpect(status().isBadRequest());
    }

    @Test
    void usersCannotSeeEachOthersBookings() throws Exception {
        String alice = register("alice@test.com");
        String bob = register("bob@test.com");
        long id = idOf(book(alice, table, slot(18), slot(20), 2).andExpect(status().isCreated()));

        mvc.perform(get("/api/bookings/" + id).with(as(alice))).andExpect(status().isOk());
        mvc.perform(get("/api/bookings/" + id).with(as(bob))).andExpect(status().isNotFound());
    }

    @Test
    void bookedTableIsMissingFromAvailableList() throws Exception {
        String user = register("u@test.com");
        book(user, table, slot(18), slot(20), 2).andExpect(status().isCreated());

        mvc.perform(get("/api/tables/available").with(as(user))
                        .param("start", slot(19).toString()).param("end", slot(21).toString()).param("partySize", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/tables/available").with(as(user))
                        .param("start", slot(20).toString()).param("end", slot(22).toString()).param("partySize", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void cancelledBookingFreesTheSlot() throws Exception {
        String user = register("u@test.com");
        long id = idOf(book(user, table, slot(18), slot(20), 2).andExpect(status().isCreated()));

        mvc.perform(delete("/api/bookings/" + id).with(as(user))).andExpect(status().isNoContent());
        book(user, table, slot(18), slot(20), 2).andExpect(status().isCreated());
    }

    @Test
    void userCannotCancelWithinTwoHoursButAdminCan() throws Exception {
        String user = register("u@test.com");
        Instant start = Instant.now().plus(Duration.ofHours(1));
        long id = idOf(book(user, table, start, start.plus(Duration.ofHours(1)), 2).andExpect(status().isCreated()));

        mvc.perform(delete("/api/bookings/" + id).with(as(user))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/bookings/" + id).with(as(admin))).andExpect(status().isNoContent());
    }

    @Test
    void adminListSupportsPaginationAndFilters() throws Exception {
        String user = register("u@test.com");
        for (int i = 0; i < 3; i++) {
            book(user, table, slot(10 + 3 * i), slot(12 + 3 * i), 2).andExpect(status().isCreated());
        }

        mvc.perform(get("/api/bookings").with(as(admin)).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(get("/api/bookings").with(as(admin)).param("status", "CANCELLED"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/bookings").with(as(admin)).param("email", "U@TEST"))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/bookings").with(as(admin)).param("status", "nonsense"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/bookings").with(as(user))).andExpect(status().isForbidden());
    }
}