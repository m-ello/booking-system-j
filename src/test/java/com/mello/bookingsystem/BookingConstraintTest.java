package com.mello.bookingsystem;

import com.mello.bookingsystem.booking.Booking;
import com.mello.bookingsystem.booking.BookingStatus;
import com.mello.bookingsystem.diningtable.DiningTable;
import com.mello.bookingsystem.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BookingConstraintTest extends AbstractIntegrationTest {

    User user;
    DiningTable table;

    @BeforeEach
    void data() {
        user = new User();
        user.setEmail("c@test.com");
        user.setPasswordHash("x");
        user = users.save(user);

        table = new DiningTable();
        table.setLabel("C1");
        table.setSeats(4);
        table = tables.save(table);

        bookings.saveAndFlush(booking(slot(18), slot(20), BookingStatus.CONFIRMED));
    }

    private Booking booking(Instant start, Instant end, BookingStatus status) {
        Booking b = new Booking();
        b.setUser(user);
        b.setTable(table);
        b.setStartTime(start);
        b.setEndTime(end);
        b.setPartySize(2);
        b.setStatus(status);
        return b;
    }

    @Test
    void overlapQueryHandlesEdgeCases() {
        assertTrue(bookings.existsOverlap(table.getId(), slot(18), slot(20)));   // identical
        assertTrue(bookings.existsOverlap(table.getId(), slot(19), slot(21)));   // overlaps the end
        assertTrue(bookings.existsOverlap(table.getId(), slot(17), slot(19)));   // overlaps the start
        assertTrue(bookings.existsOverlap(table.getId(), slot(17), slot(21)));   // contains it
        assertTrue(bookings.existsOverlap(table.getId(), slot(19), slot(20)));   // inside it
        assertFalse(bookings.existsOverlap(table.getId(), slot(20), slot(22)));  // starts when it ends
        assertFalse(bookings.existsOverlap(table.getId(), slot(16), slot(18)));  // ends when it starts
    }

    @Test
    void cancelledBookingsAreIgnoredByTheQuery() {
        bookings.deleteAll();
        bookings.saveAndFlush(booking(slot(18), slot(20), BookingStatus.CANCELLED));
        assertFalse(bookings.existsOverlap(table.getId(), slot(18), slot(20)));
    }

    @Test
    void databaseRejectsOverlapEvenWithoutTheServiceCheck() {
        assertThrows(DataIntegrityViolationException.class,
                () -> bookings.saveAndFlush(booking(slot(19), slot(21), BookingStatus.CONFIRMED)));
    }

    @Test
    void databaseAllowsOverlapWithACancelledBooking() {
        assertDoesNotThrow(
                () -> bookings.saveAndFlush(booking(slot(19), slot(21), BookingStatus.CANCELLED)));
    }
}